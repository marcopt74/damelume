package com.marcopt74.damelume.ui.timer

import android.provider.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w480dp-h1200dp")
class ReducedMotionTest {
    @get:Rule
    val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test
    fun flippingReducedMotionWhileTheDotIsShown_doesNotCrash() {
        val running =
            TimerUiState(
                isRunning = true,
                status = TimerStatus.WORKING,
                primary = PrimaryControl.PAUSE,
                timeText = "25:00",
            )
        val state = androidx.compose.runtime.mutableStateOf(true)
        compose.setContent {
            TimerContent(
                state = running,
                reducedMotion = state.value,
                onAction = {},
                onFrame = {},
            )
        }
        compose.onNodeWithContentDescription("Working, 25:00 left").assertExists()
        compose.mainClock.autoAdvance = false
        state.value = false
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithContentDescription("Working, 25:00 left").assertExists()
        state.value = true
        compose.mainClock.advanceTimeBy(32)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Working, 25:00 left").assertExists()
    }

    @Test
    fun animatorScaleObserver_flipsTheReducedMotionFlag() {
        val seen = mutableListOf<Boolean>()
        compose.setContent {
            val reduced = rememberReducedMotion()
            LaunchedEffect(reduced) { seen += reduced }
            Text(if (reduced) "reduced" else "motion")
        }
        compose.waitForIdle()
        val resolver = compose.activity.contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        resolver.notifyChange(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), null)
        compose.waitForIdle()
        assertThat(seen).contains(true)
    }
}
