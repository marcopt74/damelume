package com.marcopt74.damelume.domain

import com.marcopt74.damelume.domain.model.AlarmCommand
import com.marcopt74.damelume.domain.model.EngineResult
import com.marcopt74.damelume.domain.model.Mode
import com.marcopt74.damelume.domain.model.OngoingCommand
import com.marcopt74.damelume.domain.model.SessionDraft
import com.marcopt74.damelume.domain.model.TimerAction
import com.marcopt74.damelume.domain.model.TimerState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object TimerEngine {
    const val COMPLETE_THRESHOLD_MS = 200L
    const val SOUND_WINDOW_MS = 120_000L
    const val WORK_MIN = 1
    const val WORK_MAX = 90
    const val BREAK_MIN = 1
    const val BREAK_MAX = 30
    const val LONG_BREAK_MIN = 1
    const val LONG_BREAK_MAX = 60
    const val CYCLE_LENGTH = 4

    fun durationMs(state: TimerState): Long =
        when (state.mode) {
            Mode.WORK -> state.workMinutes * 60_000L
            Mode.BREAK -> state.breakMinutes * 60_000L
            Mode.LONG_BREAK -> state.longBreakMinutes * 60_000L
        }

    fun displayedRemainingMs(
        state: TimerState,
        nowMs: Long,
    ): Long {
        val endsAt = state.endsAtEpochMs
        return if (state.isRunning && endsAt != null) {
            (endsAt - nowMs).coerceAtLeast(0L)
        } else {
            state.remainingMs.coerceAtLeast(0L)
        }
    }

    fun isMidPause(state: TimerState): Boolean {
        if (state.isRunning) return false
        val total = durationMs(state)
        return state.remainingMs in 1 until total
    }

    /**
     * `mode == LONG_BREAK` iff `cycleWorkCount == 4`. Mode wins when a persisted
     * snapshot breaks the invariant: a long break is forced to 4, and a cycle of 4
     * outside a long break is cleared.
     */
    fun repairCycle(state: TimerState): TimerState {
        val clamped = state.cycleWorkCount.coerceIn(0, CYCLE_LENGTH)
        val cycle =
            when {
                state.mode == Mode.LONG_BREAK && clamped != CYCLE_LENGTH -> CYCLE_LENGTH
                state.mode != Mode.LONG_BREAK && clamped == CYCLE_LENGTH -> 0
                else -> clamped
            }
        return if (cycle == state.cycleWorkCount) state else state.copy(cycleWorkCount = cycle)
    }

    fun reduce(
        state: TimerState,
        action: TimerAction,
        nowMs: Long,
        soundEnabled: Boolean,
        zone: ZoneId,
    ): EngineResult {
        val repaired = repairCycle(state)
        return when (action) {
            TimerAction.Start -> start(repaired, nowMs)
            TimerAction.Pause -> pause(repaired, nowMs)
            TimerAction.Reset -> reset(repaired)
            TimerAction.SkipBreak -> skip(repaired)
            is TimerAction.Complete ->
                complete(
                    state = repaired,
                    nowMs = nowMs,
                    soundEnabled = soundEnabled,
                    zone = zone,
                    expectedEndsAt = action.expectedEndsAt,
                    countOnReconcileDay = false,
                )
            TimerAction.ResetSessions -> resetSessions(repaired, nowMs)
            is TimerAction.SetWorkMinutes -> setMinutes(repaired, Mode.WORK, action.minutes)
            is TimerAction.SetBreakMinutes -> setMinutes(repaired, Mode.BREAK, action.minutes)
            is TimerAction.SetLongBreakMinutes -> setMinutes(repaired, Mode.LONG_BREAK, action.minutes)
            TimerAction.Reconcile -> reconcile(repaired, nowMs, soundEnabled, zone)
            is TimerAction.SetSoundEnabled,
            is TimerAction.SetLocale,
            TimerAction.MarkNotificationAsked,
            TimerAction.DismissNotificationBanner,
            -> EngineResult(state = repaired)
        }
    }

    private fun start(
        state: TimerState,
        nowMs: Long,
    ): EngineResult {
        if (state.isRunning) return EngineResult(state = state)
        val remaining = if (state.remainingMs > 0L) state.remainingMs else durationMs(state)
        val endsAt = nowMs + remaining
        val next = state.copy(isRunning = true, remainingMs = remaining, endsAtEpochMs = endsAt)
        return EngineResult(
            state = next,
            alarm = AlarmCommand.Schedule(endsAt),
            ongoing = OngoingCommand.Update,
        )
    }

    private fun pause(
        state: TimerState,
        nowMs: Long,
    ): EngineResult {
        if (!state.isRunning) return EngineResult(state = state)
        val endsAt = state.endsAtEpochMs
        val remaining = if (endsAt != null) (endsAt - nowMs).coerceAtLeast(0L) else state.remainingMs
        val next = state.copy(isRunning = false, remainingMs = remaining, endsAtEpochMs = null)
        return EngineResult(
            state = next,
            alarm = AlarmCommand.Cancel,
            ongoing = if (isMidPause(next)) OngoingCommand.Update else OngoingCommand.Cancel,
        )
    }

    private fun reset(state: TimerState): EngineResult {
        val next =
            state.copy(
                isRunning = false,
                endsAtEpochMs = null,
                remainingMs = durationMs(state),
            )
        return EngineResult(
            state = next,
            alarm = AlarmCommand.Cancel,
            ongoing = OngoingCommand.Cancel,
        )
    }

    private fun skip(state: TimerState): EngineResult {
        if (state.mode != Mode.BREAK && state.mode != Mode.LONG_BREAK) {
            return EngineResult(state = state)
        }
        val cycle = if (state.mode == Mode.LONG_BREAK) 0 else state.cycleWorkCount
        val next =
            state.copy(
                mode = Mode.WORK,
                isRunning = false,
                endsAtEpochMs = null,
                remainingMs = state.workMinutes * 60_000L,
                cycleWorkCount = cycle,
            )
        return EngineResult(
            state = next,
            alarm = AlarmCommand.Cancel,
            ongoing = OngoingCommand.Cancel,
        )
    }

    private fun complete(
        state: TimerState,
        nowMs: Long,
        soundEnabled: Boolean,
        zone: ZoneId,
        expectedEndsAt: Long?,
        countOnReconcileDay: Boolean,
    ): EngineResult {
        if (expectedEndsAt != null && state.endsAtEpochMs != expectedEndsAt) {
            return EngineResult(state = state)
        }
        val remaining = displayedRemainingMs(state, nowMs)
        if (remaining > COMPLETE_THRESHOLD_MS) return EngineResult(state = state)
        if (!state.isRunning && state.endsAtEpochMs == null) {
            return EngineResult(state = state)
        }
        val endedAt = state.endsAtEpochMs ?: nowMs
        val late = nowMs - endedAt >= SOUND_WINDOW_MS
        val endedDay = localDate(endedAt, zone)
        val today = localDate(nowMs, zone)
        // AC-28: a phase that ended yesterday and is reconciled today still counts today.
        // An alarm that fires on its own keeps the calendar date of endedAt.
        val bucket = if (countOnReconcileDay && endedDay.isBefore(today)) today else endedDay
        val finished = state.mode
        val planned = durationMs(state)
        val (nextMode, nextCycle) =
            when (finished) {
                Mode.WORK -> {
                    val cycle = state.cycleWorkCount + 1
                    if (cycle >= CYCLE_LENGTH) Mode.LONG_BREAK to CYCLE_LENGTH else Mode.BREAK to cycle
                }
                Mode.BREAK -> Mode.WORK to state.cycleWorkCount
                Mode.LONG_BREAK -> Mode.WORK to 0
            }
        val next =
            state.copy(
                mode = nextMode,
                isRunning = false,
                endsAtEpochMs = null,
                remainingMs = minutesOf(state, nextMode) * 60_000L,
                cycleWorkCount = nextCycle,
            )
        return EngineResult(
            state = next,
            session =
                SessionDraft(
                    mode = finished,
                    plannedMs = planned,
                    endedAtEpochMs = endedAt,
                    localDate = bucket,
                    lateCompletion = late,
                ),
            playChime = soundEnabled && !late,
            alarm = AlarmCommand.Cancel,
            ongoing = OngoingCommand.Cancel,
        )
    }

    private fun resetSessions(
        state: TimerState,
        nowMs: Long,
    ): EngineResult {
        val leavingLongBreak = state.mode == Mode.LONG_BREAK
        val next =
            if (leavingLongBreak) {
                state.copy(
                    mode = Mode.WORK,
                    isRunning = false,
                    endsAtEpochMs = null,
                    remainingMs = state.workMinutes * 60_000L,
                    cycleWorkCount = 0,
                )
            } else {
                state.copy(cycleWorkCount = 0)
            }
        return EngineResult(
            state = next,
            alarm = if (leavingLongBreak) AlarmCommand.Cancel else AlarmCommand.Unchanged,
            ongoing =
                if (leavingLongBreak) {
                    OngoingCommand.Cancel
                } else if (isMidPause(next) || next.isRunning) {
                    OngoingCommand.Update
                } else {
                    OngoingCommand.Unchanged
                },
            sessionsResetAtEpochMs = nowMs,
        )
    }

    private fun setMinutes(
        state: TimerState,
        target: Mode,
        minutes: Int,
    ): EngineResult {
        val clamped = clamp(target, minutes)
        var next =
            when (target) {
                Mode.WORK -> state.copy(workMinutes = clamped)
                Mode.BREAK -> state.copy(breakMinutes = clamped)
                Mode.LONG_BREAK -> state.copy(longBreakMinutes = clamped)
            }
        if (!state.isRunning && state.mode == target) {
            next = next.copy(remainingMs = clamped * 60_000L)
        }
        val ongoing =
            if (!next.isRunning && !isMidPause(next)) {
                OngoingCommand.Cancel
            } else {
                OngoingCommand.Unchanged
            }
        return EngineResult(state = next, ongoing = ongoing)
    }

    private fun reconcile(
        state: TimerState,
        nowMs: Long,
        soundEnabled: Boolean,
        zone: ZoneId,
    ): EngineResult {
        val endsAt = state.endsAtEpochMs
        if (state.isRunning && endsAt != null) {
            if (endsAt <= nowMs) {
                return complete(
                    state = state,
                    nowMs = nowMs,
                    soundEnabled = soundEnabled,
                    zone = zone,
                    expectedEndsAt = null,
                    countOnReconcileDay = true,
                )
            }
            val next = state.copy(remainingMs = endsAt - nowMs)
            return EngineResult(
                state = next,
                alarm = AlarmCommand.Schedule(endsAt),
                ongoing = OngoingCommand.Update,
            )
        }
        val ongoing = if (isMidPause(state)) OngoingCommand.Update else OngoingCommand.Cancel
        return EngineResult(
            state = state,
            alarm = AlarmCommand.Cancel,
            ongoing = ongoing,
        )
    }

    private fun clamp(
        mode: Mode,
        minutes: Int,
    ): Int =
        when (mode) {
            Mode.WORK -> minutes.coerceIn(WORK_MIN, WORK_MAX)
            Mode.BREAK -> minutes.coerceIn(BREAK_MIN, BREAK_MAX)
            Mode.LONG_BREAK -> minutes.coerceIn(LONG_BREAK_MIN, LONG_BREAK_MAX)
        }

    private fun minutesOf(
        state: TimerState,
        mode: Mode,
    ): Int =
        when (mode) {
            Mode.WORK -> state.workMinutes
            Mode.BREAK -> state.breakMinutes
            Mode.LONG_BREAK -> state.longBreakMinutes
        }

    fun localDate(
        epochMs: Long,
        zone: ZoneId,
    ): LocalDate = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
}
