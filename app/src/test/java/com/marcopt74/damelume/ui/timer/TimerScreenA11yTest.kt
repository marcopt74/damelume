package com.marcopt74.damelume.ui.timer

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.domain.model.Mode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w480dp-h2400dp")
class TimerScreenA11yTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun controlsExposeNamesStateAndTouchTargets() {
        compose.setContent {
            TimerContent(
                state =
                    TimerUiState(
                        mode = Mode.BREAK,
                        showSkip = true,
                        status = TimerStatus.BREAK,
                        timeText = "05:00",
                    ),
                reducedMotion = true,
                onAction = {},
                onFrame = {},
            )
        }
        compose.onNodeWithContentDescription("Break, 05:00 left").assertExists()
        compose.onNodeWithContentDescription("Reset").assertExists()
        compose.onNodeWithContentDescription("Decrease work").assertExists()
        compose.onNodeWithContentDescription("Increase work").assertExists()
        compose.onNodeWithContentDescription("Decrease break").assertExists()
        compose.onNodeWithContentDescription("Increase break").assertExists()
        compose.onNodeWithContentDescription("Turn completion sound off").assertExists()
        val sound = compose.onNodeWithContentDescription("Turn completion sound off").fetchSemanticsNode()
        assertThat(sound.config.getOrNull(SemanticsProperties.ToggleableState)).isEqualTo(ToggleableState.On)
        compose.onNodeWithText("PT").assertExists()
        val pt = compose.onNodeWithText("PT").fetchSemanticsNode()
        assertThat(pt.config.getOrNull(SemanticsProperties.Selected)).isTrue()

        val density = compose.density

        fun heightOf(description: String): Float {
            val node = compose.onNodeWithContentDescription(description).fetchSemanticsNode()
            return with(density) { node.boundsInRoot.height.toDp() }.value
        }
        assertThat(heightOf("Reset")).isWithin(2f).of(56f)
        assertThat(heightOf("Decrease work")).isWithin(2f).of(36f)
        assertThat(heightOf("Turn completion sound off")).isWithin(2f).of(44f)
        val chip = compose.onNodeWithText("PT").fetchSemanticsNode()
        val chipHeight = with(density) { chip.boundsInRoot.height.toDp() }.value
        assertThat(chipHeight).isWithin(2f).of(28f)
    }

    @Test
    fun dialDescription_break() {
        compose.setContent {
            TimerContent(
                state =
                    TimerUiState(
                        mode = Mode.BREAK,
                        status = TimerStatus.RESTING,
                        timeText = "05:00",
                        showSkip = true,
                        isRunning = true,
                        primary = PrimaryControl.PAUSE,
                    ),
                reducedMotion = true,
                onAction = {},
                onFrame = {},
            )
        }
        compose.onNodeWithContentDescription("On a break, 05:00 left").assertExists()
        compose
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            .assertExists()
    }
}
