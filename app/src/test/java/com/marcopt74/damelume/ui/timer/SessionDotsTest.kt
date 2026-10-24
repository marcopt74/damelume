package com.marcopt74.damelume.ui.timer

import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.domain.model.TimerState
import org.junit.Test

class SessionDotsTest {
    @Test
    fun dotsFollowTodayCountModFour() {
        assertThat(TimerUiState.dotsFor(0)).isEqualTo(0)
        assertThat(TimerUiState.dotsFor(1)).isEqualTo(1)
        assertThat(TimerUiState.dotsFor(3)).isEqualTo(3)
        assertThat(TimerUiState.dotsFor(4)).isEqualTo(4)
        assertThat(TimerUiState.dotsFor(5)).isEqualTo(1)
        assertThat(TimerUiState.dotsFor(8)).isEqualTo(4)
        val ui =
            TimerUiState.from(
                timer = TimerState(),
                nowMs = 0L,
                todayCount = 4,
                soundEnabled = true,
                locale = null,
            )
        assertThat(ui.sessionDots).isEqualTo(4)
        assertThat(ui.resetSessionsEnabled).isTrue()
        val empty =
            TimerUiState.from(
                timer = TimerState(),
                nowMs = 0L,
                todayCount = 0,
                soundEnabled = true,
                locale = null,
            )
        assertThat(empty.resetSessionsEnabled).isFalse()
    }
}
