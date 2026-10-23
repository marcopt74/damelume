package com.marcopt74.damelume.ui.timer

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.marcopt74.damelume.domain.model.Mode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
        val dialNodes = compose.onAllNodes(hasContentDescription("Break, 05:00 left")).fetchSemanticsNodes()
        assertThat(dialNodes).hasSize(1)
        val dial = dialNodes.single()
        assertThat(dial.boundsInRoot.height).isGreaterThan(0f)
        assertThat(dial.boundsInRoot.width).isGreaterThan(0f)
        assertThat(dial.boundsInRoot.height).isWithin(2f).of(dial.boundsInRoot.width)
        assertThat(compose.onAllNodes(hasText("05:00")).fetchSemanticsNodes(false)).isEmpty()
        assertThat(compose.onAllNodes(hasText("BREAK")).fetchSemanticsNodes(false)).isEmpty()
        val liveRegions =
            dialNodes.count { node ->
                node.config.getOrNull(SemanticsProperties.LiveRegion) == androidx.compose.ui.semantics.LiveRegionMode.Polite
            }
        assertThat(liveRegions).isEqualTo(1)
        compose.onNodeWithContentDescription("Reset").assertExists()
        compose.onNodeWithContentDescription("Decrease work").assertExists()
        compose.onNodeWithContentDescription("Increase work").assertExists()
        compose.onNodeWithContentDescription("Decrease break").assertExists()
        compose.onNodeWithContentDescription("Increase break").assertExists()
        compose.onNodeWithContentDescription("Decrease long break").assertExists()
        compose.onNodeWithContentDescription("Increase long break").assertExists()
        compose.onNodeWithContentDescription("Turn completion sound off").assertExists()
        val sound = compose.onNodeWithContentDescription("Turn completion sound off").fetchSemanticsNode()
        assertThat(sound.config.getOrNull(SemanticsProperties.ToggleableState)).isEqualTo(ToggleableState.On)
        compose.onNodeWithText("PT").assertExists()
        val pt = compose.onNodeWithText("PT").fetchSemanticsNode()
        assertThat(pt.config.getOrNull(SemanticsProperties.Selected)).isTrue()

        val interactive = hasClickAction() or isToggleable() or isSelectable()
        val nodes = compose.onAllNodes(interactive, useUnmergedTree = true).fetchSemanticsNodes()
        assertThat(nodes).isNotEmpty()
        val density = compose.density
        val problems =
            nodes.mapNotNull { node ->
                val touch = node.touchBoundsInRoot
                val layout = node.boundsInRoot
                val touchHeight = with(density) { touch.height.toDp() }.value
                val touchWidth = with(density) { touch.width.toDp() }.value
                val layoutHeight = with(density) { layout.height.toDp() }.value
                val layoutWidth = with(density) { layout.width.toDp() }.value
                val height = maxOf(touchHeight, layoutHeight)
                val width = maxOf(touchWidth, layoutWidth)
                if (height == 0f && width == 0f) return@mapNotNull null
                val label =
                    node.config.getOrNull(SemanticsProperties.ContentDescription)
                        ?: node.config.getOrNull(SemanticsProperties.Text)
                if (height >= 47.5f && width >= 47.5f) {
                    null
                } else {
                    "touch=${touchWidth}x$touchHeight layout=${layoutWidth}x$layoutHeight label=$label"
                }
            }
        assertWithMessage("undersized interactive nodes").that(problems).isEmpty()
    }

    @Test
    fun dialDescription_longBreak() {
        compose.setContent {
            TimerContent(
                state =
                    TimerUiState(
                        mode = Mode.LONG_BREAK,
                        status = TimerStatus.LONG_RESTING,
                        timeText = "15:00",
                        showSkip = true,
                        isRunning = true,
                        cycleWorkCount = 4,
                        primary = PrimaryControl.PAUSE,
                    ),
                reducedMotion = true,
                onAction = {},
                onFrame = {},
            )
        }
        compose.onNodeWithContentDescription("On a long break, 15:00 left").assertExists()
        compose
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            .assertExists()
    }
}
