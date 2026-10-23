package com.marcopt74.damelume.domain.model

data class TimerState(
    val mode: Mode = Mode.WORK,
    val isRunning: Boolean = false,
    val remainingMs: Long = DEFAULT_REMAINING_MS,
    val endsAtEpochMs: Long? = null,
    val workMinutes: Int = DEFAULT_WORK_MINUTES,
    val breakMinutes: Int = DEFAULT_BREAK_MINUTES,
    val longBreakMinutes: Int = DEFAULT_LONG_BREAK_MINUTES,
    val cycleWorkCount: Int = 0,
) {
    companion object {
        const val DEFAULT_WORK_MINUTES = 25
        const val DEFAULT_BREAK_MINUTES = 5
        const val DEFAULT_LONG_BREAK_MINUTES = 15
        const val DEFAULT_REMAINING_MS = DEFAULT_WORK_MINUTES * 60_000L
    }
}
