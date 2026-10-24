package com.marcopt74.damelume.ui.timer

import com.marcopt74.damelume.domain.TimerEngine
import com.marcopt74.damelume.domain.model.AppLocale
import com.marcopt74.damelume.domain.model.Mode
import com.marcopt74.damelume.domain.model.TimerState
import com.marcopt74.damelume.ui.format.TimeFormatter

enum class TimerStatus {
    WORK,
    WORKING,
    PAUSED,
    BREAK,
    RESTING,
    BREAK_PAUSED,
}

enum class PrimaryControl {
    START,
    PAUSE,
    RESUME,
}

data class TimerUiState(
    val ready: Boolean = true,
    val mode: Mode = Mode.WORK,
    val isRunning: Boolean = false,
    val endsAtEpochMs: Long? = null,
    val remainingMs: Long = TimerState.DEFAULT_REMAINING_MS,
    val totalMs: Long = TimerState.DEFAULT_REMAINING_MS,
    val timeText: String = "25:00",
    val status: TimerStatus = TimerStatus.WORK,
    val primary: PrimaryControl = PrimaryControl.START,
    val showSkip: Boolean = false,
    val todayCount: Int = 0,
    val sessionDots: Int = 0,
    val resetSessionsEnabled: Boolean = false,
    val soundEnabled: Boolean = true,
    val workMinutes: Int = TimerState.DEFAULT_WORK_MINUTES,
    val breakMinutes: Int = TimerState.DEFAULT_BREAK_MINUTES,
    val steppersEnabled: Boolean = true,
    val showNotificationBanner: Boolean = false,
    val showNotificationRationale: Boolean = false,
    val requestNotificationOnStart: Boolean = false,
    val showExactAlarmBanner: Boolean = false,
    val locale: AppLocale? = null,
) {
    companion object {
        fun dotsFor(count: Int): Int {
            if (count <= 0) return 0
            val remainder = count % 4
            return if (remainder == 0) 4 else remainder
        }

        fun from(
            timer: TimerState,
            nowMs: Long,
            todayCount: Int,
            soundEnabled: Boolean,
            locale: AppLocale?,
            showNotificationBanner: Boolean = false,
            showNotificationRationale: Boolean = false,
            requestNotificationOnStart: Boolean = false,
            showExactAlarmBanner: Boolean = false,
        ): TimerUiState {
            val remaining = TimerEngine.displayedRemainingMs(timer, nowMs)
            val total = TimerEngine.durationMs(timer)
            val midPause = !timer.isRunning && remaining in 1 until total
            val status = statusOf(timer.mode, timer.isRunning, midPause)
            val primary =
                when {
                    timer.isRunning -> PrimaryControl.PAUSE
                    midPause -> PrimaryControl.RESUME
                    else -> PrimaryControl.START
                }
            return TimerUiState(
                ready = true,
                mode = timer.mode,
                isRunning = timer.isRunning,
                endsAtEpochMs = timer.endsAtEpochMs,
                remainingMs = remaining,
                totalMs = total,
                timeText = TimeFormatter.format(remaining),
                status = status,
                primary = primary,
                showSkip = timer.mode == Mode.BREAK,
                todayCount = todayCount,
                sessionDots = dotsFor(todayCount),
                resetSessionsEnabled = todayCount > 0,
                soundEnabled = soundEnabled,
                workMinutes = timer.workMinutes,
                breakMinutes = timer.breakMinutes,
                steppersEnabled = !timer.isRunning,
                showNotificationBanner = showNotificationBanner,
                showNotificationRationale = showNotificationRationale,
                requestNotificationOnStart = requestNotificationOnStart,
                showExactAlarmBanner = showExactAlarmBanner,
                locale = locale,
            )
        }

        fun statusOf(
            mode: Mode,
            running: Boolean,
            midPause: Boolean,
        ): TimerStatus =
            when (mode) {
                Mode.WORK ->
                    when {
                        running -> TimerStatus.WORKING
                        midPause -> TimerStatus.PAUSED
                        else -> TimerStatus.WORK
                    }
                Mode.BREAK ->
                    when {
                        running -> TimerStatus.RESTING
                        midPause -> TimerStatus.BREAK_PAUSED
                        else -> TimerStatus.BREAK
                    }
            }
    }
}
