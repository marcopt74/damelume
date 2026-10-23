package com.marcopt74.damelume.ui.timer

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.domain.model.Mode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WindowInsetsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun title_sitsBelowTheStatusBarAndCutout() {
        val statusPx = 84
        val cutoutPx = 36
        compose.activityRule.scenario.onActivity { activity ->
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        }
        compose.setContent {
            TimerContent(
                state =
                    TimerUiState(
                        mode = Mode.WORK,
                        status = TimerStatus.WORK,
                        timeText = "25:00",
                    ),
                reducedMotion = true,
                onAction = {},
                onFrame = {},
            )
        }
        val content = compose.activity.findViewById<View>(android.R.id.content)
        val insets =
            WindowInsetsCompat
                .Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, statusPx, 0, 0))
                .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(0, cutoutPx, 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, 48))
                .build()
        ViewCompat.dispatchApplyWindowInsets(compose.activity.window.decorView, insets)
        ViewCompat.dispatchApplyWindowInsets(content, insets)
        compose.waitForIdle()

        val top =
            compose
                .onNodeWithText("Lume")
                .getBoundsInRoot()
                .top.value
        val density = compose.density.density
        assertThat(top).isAtLeast(statusPx / density)
    }
}
