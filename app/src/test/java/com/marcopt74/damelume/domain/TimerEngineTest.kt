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
                runningWork(endsAt = 1_000, cycle = 1),
                TimerAction.Complete(expectedEndsAt = 1_000),
                now = 1_000,
            )
        assertThat(result.state.mode).isEqualTo(Mode.BREAK)
        assertThat(result.state.isRunning).isFalse()
        assertThat(result.state.cycleWorkCount).isEqualTo(2)
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
                    cycleWorkCount = 2,
                    workMinutes = 25,
                ),
                TimerAction.Complete(expectedEndsAt = 2_000),
                now = 2_000,
            )
        assertThat(result.state.mode).isEqualTo(Mode.WORK)
        assertThat(result.state.isRunning).isFalse()
        assertThat(result.state.cycleWorkCount).isEqualTo(2)
        assertThat(result.session!!.mode).isEqualTo(Mode.BREAK)
    }

    @Test
    fun complete_ignoredAbove200ms() {
        val state = runningWork(endsAt = 10_000, cycle = 0)
        val result = reduce(state, TimerAction.Complete(expectedEndsAt = 10_000), now = 9_000)
        assertThat(result.state).isEqualTo(state)
        assertThat(result.session).isNull()
    }

    @Test
    fun skipBreak() {
        val result =
            reduce(
                TimerState(mode = Mode.BREAK, isRunning = true, endsAtEpochMs = 50, cycleWorkCount = 2, workMinutes = 25),
                TimerAction.SkipBreak,
            )
        assertThat(result.state.mode).isEqualTo(Mode.WORK)
        assertThat(result.state.isRunning).isFalse()
        assertThat(result.state.remainingMs).isEqualTo(25 * 60_000L)
        assertThat(result.state.cycleWorkCount).isEqualTo(2)
        assertThat(result.session).isNull()
        val ignored = reduce(TimerState(mode = Mode.WORK, cycleWorkCount = 1), TimerAction.SkipBreak)
        assertThat(ignored.state.mode).isEqualTo(Mode.WORK)
        assertThat(ignored.state.cycleWorkCount).isEqualTo(1)
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
                runningWork(endsAt = 50_000, cycle = 1),
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
                runningWork(endsAt = endsAt, cycle = 0),
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
                runningWork(endsAt = endsAt, cycle = 0),
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
                runningWork(endsAt = endsAt, cycle = 2),
                TimerAction.Reconcile,
                now = now,
            )
        assertThat(result.session!!.endedAtEpochMs).isEqualTo(endsAt)
        assertThat(result.session!!.localDate).isEqualTo(LocalDate.of(2026, 10, 23))
        assertThat(result.state.cycleWorkCount).isEqualTo(3)
        assertThat(result.state.mode).isEqualTo(Mode.BREAK)
        assertThat(result.playChime).isFalse()
    }

    @Test
    fun completeFourthWork_goesToLongBreak_cycle4() {
        val result =
            reduce(
                runningWork(endsAt = 5_000, cycle = 3),
                TimerAction.Complete(expectedEndsAt = 5_000),
                now = 5_000,
            )
        assertThat(result.state.mode).isEqualTo(Mode.LONG_BREAK)
        assertThat(result.state.cycleWorkCount).isEqualTo(4)
        assertThat(result.state.isRunning).isFalse()
        assertThat(result.state.remainingMs).isEqualTo(15 * 60_000L)
        assertThat(result.session!!.mode).isEqualTo(Mode.WORK)
    }

    @Test
    fun completeLongBreak_resetsCycle_noAutoStart() {
        val result =
            reduce(
                TimerState(
                    mode = Mode.LONG_BREAK,
                    isRunning = true,
                    endsAtEpochMs = 8_000,
                    cycleWorkCount = 4,
                    workMinutes = 25,
                ),
                TimerAction.Complete(expectedEndsAt = 8_000),
                now = 8_000,
            )
        assertThat(result.state.mode).isEqualTo(Mode.WORK)
        assertThat(result.state.isRunning).isFalse()
        assertThat(result.state.cycleWorkCount).isEqualTo(0)
        assertThat(result.state.remainingMs).isEqualTo(25 * 60_000L)
        assertThat(result.session!!.mode).isEqualTo(Mode.LONG_BREAK)
    }

    @Test
    fun completeWork_cycleBelow3_goesToShortBreak() {
        val result =
            reduce(
                runningWork(endsAt = 3_000, cycle = 2),
                TimerAction.Complete(expectedEndsAt = 3_000),
                now = 3_000,
            )
        assertThat(result.state.mode).isEqualTo(Mode.BREAK)
        assertThat(result.state.cycleWorkCount).isEqualTo(3)
    }

    @Test
    fun skipLongBreak_resetsCycle() {
        val result =
            reduce(
                TimerState(mode = Mode.LONG_BREAK, cycleWorkCount = 4, workMinutes = 25, isRunning = true, endsAtEpochMs = 10),
                TimerAction.SkipBreak,
            )
        assertThat(result.state.mode).isEqualTo(Mode.WORK)
        assertThat(result.state.cycleWorkCount).isEqualTo(0)
        assertThat(result.state.isRunning).isFalse()
        assertThat(result.state.remainingMs).isEqualTo(25 * 60_000L)
        assertThat(result.alarm).isEqualTo(AlarmCommand.Cancel)
    }

    @Test
    fun setLongBreak_clamps_1_60() {
        assertThat(reduce(TimerState(), TimerAction.SetLongBreakMinutes(0)).state.longBreakMinutes).isEqualTo(1)
        assertThat(reduce(TimerState(), TimerAction.SetLongBreakMinutes(60)).state.longBreakMinutes).isEqualTo(60)
        assertThat(reduce(TimerState(), TimerAction.SetLongBreakMinutes(61)).state.longBreakMinutes).isEqualTo(60)
    }

    @Test
    fun setLongBreak_resetsRemainingWhenCurrentAndStopped() {
        val stopped = TimerState(mode = Mode.LONG_BREAK, cycleWorkCount = 4, remainingMs = 4_000, longBreakMinutes = 15)
        val updated = reduce(stopped, TimerAction.SetLongBreakMinutes(20))
        assertThat(updated.state.remainingMs).isEqualTo(20 * 60_000L)
        val other = reduce(TimerState(mode = Mode.WORK, remainingMs = 9_000), TimerAction.SetLongBreakMinutes(20))
        assertThat(other.state.remainingMs).isEqualTo(9_000)
    }

    @Test
    fun resetSessions_inLongBreak_returnsToWork() {
        val result =
            reduce(
                TimerState(mode = Mode.LONG_BREAK, cycleWorkCount = 4, isRunning = true, endsAtEpochMs = 70, workMinutes = 25),
                TimerAction.ResetSessions,
                now = 80,
            )
        assertThat(result.state.mode).isEqualTo(Mode.WORK)
        assertThat(result.state.cycleWorkCount).isEqualTo(0)
        assertThat(result.state.isRunning).isFalse()
        assertThat(result.state.remainingMs).isEqualTo(25 * 60_000L)
        assertThat(result.alarm).isEqualTo(AlarmCommand.Cancel)
        assertThat(result.sessionsResetAtEpochMs).isEqualTo(80)
    }

    @Test
    fun resetSessions_resetsCycle() {
        val result =
            reduce(
                TimerState(mode = Mode.BREAK, cycleWorkCount = 2, isRunning = true, endsAtEpochMs = 10),
                TimerAction.ResetSessions,
                now = 11,
            )
        assertThat(result.state.cycleWorkCount).isEqualTo(0)
        assertThat(result.state.mode).isEqualTo(Mode.BREAK)
        assertThat(result.state.isRunning).isTrue()
    }

    @Test
    fun completeFromAlarm_keepsTheEndedAtDate() {
        val endsAt = epoch(2026, 10, 22, 23, 0)
        val now = epoch(2026, 10, 23, 0, 1)
        val result = reduce(runningWork(endsAt = endsAt, cycle = 0), TimerAction.Complete(expectedEndsAt = endsAt), now = now)
        assertThat(result.session!!.endedAtEpochMs).isEqualTo(endsAt)
        assertThat(result.session!!.localDate).isEqualTo(LocalDate.of(2026, 10, 22))
    }

    @Test
    fun reconcile_newDay_keepsCycle_resetsDaily() {
        val state = TimerState(mode = Mode.WORK, isRunning = false, cycleWorkCount = 2, remainingMs = 25 * 60_000L)
        val result = reduce(state, TimerAction.Reconcile, now = epoch(2026, 10, 23, 8, 0))
        assertThat(result.state.cycleWorkCount).isEqualTo(2)
        assertThat(result.session).isNull()
        assertThat(result.state.mode).isEqualTo(Mode.WORK)
    }

    @Test
    fun reconcile_repairsCycleInvariant() {
        val broken = TimerState(mode = Mode.LONG_BREAK, cycleWorkCount = 1, isRunning = false)
        val result = reduce(broken, TimerAction.Reconcile)
        assertThat(result.state.mode).isEqualTo(Mode.LONG_BREAK)
        assertThat(result.state.cycleWorkCount).isEqualTo(4)
        val otherWay = TimerState(mode = Mode.WORK, cycleWorkCount = 4)
        assertThat(reduce(otherWay, TimerAction.Reconcile).state.cycleWorkCount).isEqualTo(0)
    }

    @Test
    fun reconcile_longBreakPast_resetsCycle() {
        val endsAt = 20_000L
        val result =
            reduce(
                TimerState(mode = Mode.LONG_BREAK, cycleWorkCount = 4, isRunning = true, endsAtEpochMs = endsAt, workMinutes = 25),
                TimerAction.Reconcile,
                now = endsAt + 1_000,
            )
        assertThat(result.state.mode).isEqualTo(Mode.WORK)
        assertThat(result.state.cycleWorkCount).isEqualTo(0)
        assertThat(result.state.isRunning).isFalse()
        assertThat(result.playChime).isTrue()
    }

    private fun runningWork(
        endsAt: Long,
        cycle: Int,
    ) = TimerState(
        mode = Mode.WORK,
        isRunning = true,
        endsAtEpochMs = endsAt,
        remainingMs = 1_000,
        cycleWorkCount = cycle,
        workMinutes = 25,
        breakMinutes = 5,
        longBreakMinutes = 15,
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
