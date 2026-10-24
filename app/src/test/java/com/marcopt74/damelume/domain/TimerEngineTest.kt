package com.marcopt74.damelume.domain

import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.domain.model.AlarmCommand
import com.marcopt74.damelume.domain.model.Mode
import com.marcopt74.damelume.domain.model.TimerAction
import com.marcopt74.damelume.domain.model.TimerState
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class TimerEngineTest {
    private val zone: ZoneId = ZoneId.of("Europe/Lisbon")

    @Test
    fun start_setsEndsAt() {
        val now = 5_000L
        val result = reduce(TimerState(remainingMs = 10_000), TimerAction.Start, now)
        assertThat(result.state.isRunning).isTrue()
        assertThat(result.state.endsAtEpochMs).isEqualTo(now + 10_000)
        assertThat(result.state.remainingMs).isEqualTo(10_000)
        assertThat(result.alarm).isEqualTo(AlarmCommand.Schedule(now + 10_000))
    }

    @Test
    fun pause_keepsRemaining() {
        val result =
            reduce(
                TimerState(isRunning = true, endsAtEpochMs = 20_000, remainingMs = 15_000),
                TimerAction.Pause,
                now = 8_000,
            )
        assertThat(result.state.isRunning).isFalse()
        assertThat(result.state.endsAtEpochMs).isNull()
        assertThat(result.state.remainingMs).isEqualTo(12_000)
        assertThat(result.alarm).isEqualTo(AlarmCommand.Cancel)
    }

    @Test
    fun reset_keepsMode() {
        val result =
            reduce(
                TimerState(mode = Mode.BREAK, isRunning = true, endsAtEpochMs = 100, breakMinutes = 7, remainingMs = 1_000),
                TimerAction.Reset,
            )
        assertThat(result.state.mode).isEqualTo(Mode.BREAK)
        assertThat(result.state.isRunning).isFalse()
        assertThat(result.state.remainingMs).isEqualTo(7 * 60_000L)
        assertThat(result.state.endsAtEpochMs).isNull()
    }

    @Test
    fun completeWork_switchesToBreak_incrementsCount() {
        val result =
            reduce(
                runningWork(endsAt = 1_000),
                TimerAction.Complete(expectedEndsAt = 1_000),
                now = 1_000,
            )
        assertThat(result.state.mode).isEqualTo(Mode.BREAK)
        assertThat(result.state.isRunning).isFalse()
        assertThat(result.state.remainingMs).isEqualTo(5 * 60_000L)
        assertThat(result.session!!.mode).isEqualTo(Mode.WORK)
        assertThat(result.playChime).isTrue()
    }

    @Test
    fun completeBreak_noIncrement() {
        val result =
            reduce(
                TimerState(
                    mode = Mode.BREAK,
                    isRunning = true,
                    endsAtEpochMs = 2_000,
                    workMinutes = 25,
                ),
                TimerAction.Complete(expectedEndsAt = 2_000),
                now = 2_000,
            )
        assertThat(result.state.mode).isEqualTo(Mode.WORK)
        assertThat(result.state.isRunning).isFalse()
        assertThat(result.session!!.mode).isEqualTo(Mode.BREAK)
    }

    @Test
    fun complete_ignoredAbove200ms() {
        val state = runningWork(endsAt = 10_000)
        val result = reduce(state, TimerAction.Complete(expectedEndsAt = 10_000), now = 9_000)
        assertThat(result.state).isEqualTo(state)
        assertThat(result.session).isNull()
    }

    @Test
    fun skipBreak() {
        val result =
            reduce(
                TimerState(mode = Mode.BREAK, isRunning = true, endsAtEpochMs = 50, workMinutes = 25),
                TimerAction.SkipBreak,
            )
        assertThat(result.state.mode).isEqualTo(Mode.WORK)
        assertThat(result.state.isRunning).isFalse()
        assertThat(result.state.remainingMs).isEqualTo(25 * 60_000L)
        assertThat(result.session).isNull()
        val ignored = reduce(TimerState(mode = Mode.WORK), TimerAction.SkipBreak)
        assertThat(ignored.state.mode).isEqualTo(Mode.WORK)
    }

    @Test
    fun setWork_clamps_1_90() {
        assertThat(reduce(TimerState(), TimerAction.SetWorkMinutes(0)).state.workMinutes).isEqualTo(1)
        assertThat(reduce(TimerState(), TimerAction.SetWorkMinutes(90)).state.workMinutes).isEqualTo(90)
        assertThat(reduce(TimerState(), TimerAction.SetWorkMinutes(91)).state.workMinutes).isEqualTo(90)
        assertThat(reduce(TimerState(), TimerAction.SetWorkMinutes(40)).state.workMinutes).isEqualTo(40)
    }

    @Test
    fun setBreak_clamps_1_30() {
        assertThat(reduce(TimerState(), TimerAction.SetBreakMinutes(0)).state.breakMinutes).isEqualTo(1)
        assertThat(reduce(TimerState(), TimerAction.SetBreakMinutes(30)).state.breakMinutes).isEqualTo(30)
        assertThat(reduce(TimerState(), TimerAction.SetBreakMinutes(31)).state.breakMinutes).isEqualTo(30)
    }

    @Test
    fun setCurrentModeMinutes_resetsRemainingWhenStopped() {
        val paused = TimerState(mode = Mode.WORK, isRunning = false, remainingMs = 10_000, workMinutes = 25)
        val result = reduce(paused, TimerAction.SetWorkMinutes(10))
        assertThat(result.state.remainingMs).isEqualTo(10 * 60_000L)
        val running = paused.copy(isRunning = true, endsAtEpochMs = 99)
        val whileRunning = reduce(running, TimerAction.SetWorkMinutes(10))
        assertThat(whileRunning.state.remainingMs).isEqualTo(10_000)
        assertThat(whileRunning.state.workMinutes).isEqualTo(10)
    }

    @Test
    fun setOtherMode_keepsRemaining() {
        val state = TimerState(mode = Mode.WORK, remainingMs = 12_000)
        val result = reduce(state, TimerAction.SetBreakMinutes(8))
        assertThat(result.state.remainingMs).isEqualTo(12_000)
        assertThat(result.state.breakMinutes).isEqualTo(8)
    }

    @Test
    fun reconcile_running_future() {
        val result =
            reduce(
                runningWork(endsAt = 50_000),
                TimerAction.Reconcile,
                now = 40_000,
            )
        assertThat(result.state.isRunning).isTrue()
        assertThat(result.state.remainingMs).isEqualTo(10_000)
        assertThat(result.state.endsAtEpochMs).isEqualTo(50_000)
        assertThat(result.session).isNull()
        assertThat(result.alarm).isEqualTo(AlarmCommand.Schedule(50_000))
    }

    @Test
    fun reconcile_past_lt120s_sound() {
        val endsAt = 1_000_000L
        val result =
            reduce(
                runningWork(endsAt = endsAt),
                TimerAction.Reconcile,
                now = endsAt + 30_000,
            )
        assertThat(result.playChime).isTrue()
        assertThat(result.session!!.lateCompletion).isFalse()
        assertThat(result.session!!.endedAtEpochMs).isEqualTo(endsAt)
        assertThat(result.state.mode).isEqualTo(Mode.BREAK)
        assertThat(result.state.isRunning).isFalse()
    }

    @Test
    fun reconcile_past_ge120s_silent() {
        val endsAt = 1_000_000L
        val result =
            reduce(
                runningWork(endsAt = endsAt),
                TimerAction.Reconcile,
                now = endsAt + 120_000,
                sound = true,
            )
        assertThat(result.playChime).isFalse()
        assertThat(result.session!!.lateCompletion).isTrue()
        assertThat(result.session!!.endedAtEpochMs).isEqualTo(endsAt)
    }

    @Test
    fun reconcile_newDay() {
        val endsAt = epoch(2026, 10, 22, 23, 0)
        val now = epoch(2026, 10, 23, 9, 0)
        val result =
            reduce(
                runningWork(endsAt = endsAt),
                TimerAction.Reconcile,
                now = now,
            )
        assertThat(result.session!!.localDate).isEqualTo(LocalDate.of(2026, 10, 22))
        assertThat(result.session!!.localDate).isNotEqualTo(LocalDate.of(2026, 10, 23))
        assertThat(result.state.mode).isEqualTo(Mode.BREAK)
        assertThat(result.playChime).isFalse()
    }

    @Test
    fun completeFourthWork_stillEntersBreak() {
        val result =
            reduce(
                runningWork(endsAt = 5_000),
                TimerAction.Complete(expectedEndsAt = 5_000),
                now = 5_000,
            )
        assertThat(result.state.mode).isEqualTo(Mode.BREAK)
        assertThat(result.state.isRunning).isFalse()
        assertThat(result.state.remainingMs).isEqualTo(5 * 60_000L)
        assertThat(result.session!!.mode).isEqualTo(Mode.WORK)
    }

    @Test
    fun resetSessions_keepsPhase() {
        val result =
            reduce(
                TimerState(mode = Mode.BREAK, isRunning = true, endsAtEpochMs = 10),
                TimerAction.ResetSessions,
                now = 11,
            )
        assertThat(result.state.mode).isEqualTo(Mode.BREAK)
        assertThat(result.state.isRunning).isTrue()
        assertThat(result.sessionsResetAtEpochMs).isEqualTo(11)
        assertThat(result.alarm).isEqualTo(AlarmCommand.Unchanged)
    }

    @Test
    fun reconcile_idle_keepsMode() {
        val state = TimerState(mode = Mode.WORK, isRunning = false, remainingMs = 25 * 60_000L)
        val result = reduce(state, TimerAction.Reconcile, now = epoch(2026, 10, 23, 8, 0))
        assertThat(result.session).isNull()
        assertThat(result.state.mode).isEqualTo(Mode.WORK)
    }

    private fun runningWork(endsAt: Long) =
        TimerState(
            mode = Mode.WORK,
            isRunning = true,
            endsAtEpochMs = endsAt,
            remainingMs = 1_000,
            workMinutes = 25,
            breakMinutes = 5,
        )

    private fun reduce(
        state: TimerState,
        action: TimerAction,
        now: Long = 1_000L,
        sound: Boolean = true,
    ) = TimerEngine.reduce(state, action, now, sound, zone)

    private fun epoch(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()
}
