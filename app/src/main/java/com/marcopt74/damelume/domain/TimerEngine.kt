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

    fun durationMs(state: TimerState): Long = minutesOf(state, state.mode) * 60_000L

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

    fun reduce(
        state: TimerState,
        action: TimerAction,
        nowMs: Long,
        soundEnabled: Boolean,
        zone: ZoneId,
    ): EngineResult =
        when (action) {
            TimerAction.Start -> start(state, nowMs)
            TimerAction.Pause -> pause(state, nowMs)
            TimerAction.Reset -> reset(state)
            TimerAction.SkipBreak -> skip(state)
            is TimerAction.Complete ->
                complete(
                    state = state,
                    nowMs = nowMs,
                    soundEnabled = soundEnabled,
                    zone = zone,
                    expectedEndsAt = action.expectedEndsAt,
                )
            TimerAction.ResetSessions -> resetSessions(state, nowMs)
            is TimerAction.SetWorkMinutes -> setMinutes(state, Mode.WORK, action.minutes)
            is TimerAction.SetBreakMinutes -> setMinutes(state, Mode.BREAK, action.minutes)
            TimerAction.Reconcile -> reconcile(state, nowMs, soundEnabled, zone)
            is TimerAction.SetSoundEnabled,
            is TimerAction.SetLocale,
            TimerAction.MarkNotificationAsked,
            TimerAction.DismissNotificationBanner,
            -> EngineResult(state = state)
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
        if (state.mode != Mode.BREAK) return EngineResult(state = state)
        val next =
            state.copy(
                mode = Mode.WORK,
                isRunning = false,
                endsAtEpochMs = null,
                remainingMs = state.workMinutes * 60_000L,
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
        val finished = state.mode
        val nextMode = if (finished == Mode.WORK) Mode.BREAK else Mode.WORK
        val nextDuration = minutesOf(state, nextMode) * 60_000L
        val nextEndsAt = if (late) null else nowMs + nextDuration
        val next =
            state.copy(
                mode = nextMode,
                isRunning = nextEndsAt != null,
                endsAtEpochMs = nextEndsAt,
                remainingMs = nextDuration,
            )
        return EngineResult(
            state = next,
            session =
                SessionDraft(
                    mode = finished,
                    plannedMs = durationMs(state),
                    endedAtEpochMs = endedAt,
                    localDate = localDate(endedAt, zone),
                    lateCompletion = late,
                ),
            playChime = soundEnabled && nextEndsAt != null,
            alarm = if (nextEndsAt != null) AlarmCommand.Schedule(nextEndsAt) else AlarmCommand.Cancel,
            ongoing = if (nextEndsAt != null) OngoingCommand.Update else OngoingCommand.Cancel,
        )
    }

    private fun resetSessions(
        state: TimerState,
        nowMs: Long,
    ): EngineResult =
        EngineResult(
            state = state,
            ongoing =
                if (isMidPause(state) || state.isRunning) {
                    OngoingCommand.Update
                } else {
                    OngoingCommand.Unchanged
                },
            sessionsResetAtEpochMs = nowMs,
        )

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
        }

    private fun minutesOf(
        state: TimerState,
        mode: Mode,
    ): Int =
        when (mode) {
            Mode.WORK -> state.workMinutes
            Mode.BREAK -> state.breakMinutes
        }

    fun localDate(
        epochMs: Long,
        zone: ZoneId,
    ): LocalDate = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
}
