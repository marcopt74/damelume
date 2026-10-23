package com.marcopt74.damelume.ui.timer

import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.domain.model.Mode
import com.marcopt74.damelume.domain.model.TimerState
import org.junit.Test

class TimerViewModelTest {
    @Test
    fun remainingDerivedFromEndsAt() {
        val now = 1_000_000L
        val stored =
            TimerState(
                mode = Mode.WORK,
                isRunning = true,
                endsAtEpochMs = now + 5_400_000,
                remainingMs = 1,
                workMinutes = 90,
            )
        val atStart = TimerUiState.from(stored, nowMs = now, todayCount = 0, soundEnabled = true, locale = null)
        assertThat(atStart.timeText).isEqualTo("90:00")
        val oneMinuteLater =
            TimerUiState.from(
                stored,
                nowMs = now + 60_000,
                todayCount = 0,
                soundEnabled = true,
                locale = null,
            )
        assertThat(oneMinuteLater.timeText).isEqualTo("89:00")
        assertThat(oneMinuteLater.remainingMs).isEqualTo(5_340_000)
    }
}
