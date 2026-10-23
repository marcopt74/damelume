package com.marcopt74.damelume.ui.timer

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.marcopt74.damelume.domain.model.Mode
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Locale

/**
 * Opt-in stills of the main screen. CI leaves LUME_SCREENSHOTS unset, so this
 * does not run there. Local runs write PNGs under /opt/cursor/artifacts/screenshots.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-port")
class TimerScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun captureMainScreenModes() {
        assumeTrue(System.getenv("LUME_SCREENSHOTS") == "1")
        val output = File("/opt/cursor/artifacts/screenshots")
        output.mkdirs()
        val state = mutableStateOf(frame("work", "idle"))
        val context = localizedPortuguese()
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalActivityResultRegistryOwner provides compose.activity,
            ) {
                TimerContent(
                    state = state.value,
                    reducedMotion = true,
                    onAction = {},
                    onFrame = {},
                )
            }
        }
        frames.forEach { (name, next) ->
            state.value = next
            compose.waitForIdle()
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            File(output, "$name.png").outputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
    }

    private fun localizedPortuguese(): Context {
        val configuration = Configuration(compose.activity.resources.configuration)
        configuration.setLocales(LocaleList(Locale.forLanguageTag("pt-PT")))
        return compose.activity.createConfigurationContext(configuration)
    }

    private val frames: List<Pair<String, TimerUiState>> =
        listOf(
            "work-idle" to frame("work", "idle"),
            "work-running" to frame("work", "running"),
            "break-idle" to frame("break", "idle"),
            "break-running" to frame("break", "running"),
            "long-break-idle" to frame("long", "idle"),
            "long-break-running" to frame("long", "running"),
        )

    private fun frame(
        modeName: String,
        phase: String,
    ): TimerUiState {
        val mode =
            when (modeName) {
                "break" -> Mode.BREAK
                "long" -> Mode.LONG_BREAK
                else -> Mode.WORK
            }
        val totalMinutes =
            when (mode) {
                Mode.BREAK -> 5
                Mode.LONG_BREAK -> 15
                Mode.WORK -> 25
            }
        val running = phase == "running"
        val shownMinutes = if (running) totalMinutes * 3 / 5 else totalMinutes
        val totalMs = totalMinutes * 60_000L
        val remainingMs = shownMinutes * 60_000L
        val status =
            when {
                mode == Mode.WORK && running -> TimerStatus.WORKING
                mode == Mode.WORK -> TimerStatus.WORK
                mode == Mode.BREAK && running -> TimerStatus.RESTING
                mode == Mode.BREAK -> TimerStatus.BREAK
                running -> TimerStatus.LONG_RESTING
                else -> TimerStatus.LONG_BREAK
            }
        return TimerUiState(
            mode = mode,
            isRunning = running,
            remainingMs = remainingMs,
            totalMs = totalMs,
            timeText = "%02d:00".format(shownMinutes),
            status = status,
            primary = if (running) PrimaryControl.PAUSE else PrimaryControl.START,
            showSkip = mode != Mode.WORK,
            steppersEnabled = !running,
            workMinutes = 25,
            breakMinutes = 5,
            longBreakMinutes = 15,
            cycleWorkCount = if (mode == Mode.LONG_BREAK) 4 else 0,
        )
    }
}
