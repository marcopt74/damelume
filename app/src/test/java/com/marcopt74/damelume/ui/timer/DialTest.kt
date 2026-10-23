package com.marcopt74.damelume.ui.timer

import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DialTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun reducedMotion_noContinuousAnimation() {
        var frames = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TimerContent(
                state =
                    TimerUiState(
                        isRunning = true,
                        endsAtEpochMs = 60_000,
                        remainingMs = 60_000,
                        timeText = "01:00",
                        status = TimerStatus.WORKING,
                        primary = PrimaryControl.PAUSE,
                    ),
                reducedMotion = true,
                onAction = {},
                onFrame = { frames += 1 },
            )
        }
        compose.mainClock.advanceTimeBy(2_000)
        assertThat(frames).isAtMost(4)
        assertThat(frames).isAtLeast(1)
    }
}
