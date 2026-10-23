package com.marcopt74.damelume.ui.timer

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.core.app.ActivityOptionsCompat
import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.R
import com.marcopt74.damelume.domain.model.Mode
import com.marcopt74.damelume.domain.model.TimerAction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w480dp-h2400dp")
class TimerScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun initialState_showsDefaults() {
        show(TimerUiState(), "pt-PT")
        val pt = context("pt-PT")
        compose.onNodeWithContentDescription("Trabalho, 25:00 restantes").assertIsDisplayed()
        compose.onNodeWithText(pt.getString(R.string.start)).assertIsDisplayed()
        compose.onNodeWithText("0").assertIsDisplayed()
        compose
            .onNodeWithText(pt.resources.getQuantityString(R.plurals.session_word, 0), substring = false)
            .assertIsDisplayed()
        compose.onNodeWithText(pt.getString(R.string.reset_sessions)).assertIsNotEnabled()
        compose.onNode(hasContentDescription(pt.getString(R.string.sound_off))).assertIsOn()
        compose.onNodeWithText("25").assertIsDisplayed()
        compose.onNodeWithText("5").assertIsDisplayed()
        compose.onNodeWithText("15").assertIsDisplayed()
        assertThat(
            compose.onAllNodes(hasText(pt.getString(R.string.minutes))).fetchSemanticsNodes().size,
        ).isEqualTo(3)
        assertAbsent(pt.getString(R.string.skip_break))
    }

    @Test
    fun skipBreak_onlyInBreak() {
        show(TimerUiState(mode = Mode.WORK), "en")
        assertAbsent("Skip break")
        update(TimerUiState(mode = Mode.BREAK, showSkip = true, status = TimerStatus.BREAK))
        compose.onNodeWithText("Skip break").assertIsDisplayed()
    }

    @Test
    fun skipBreak_visibleInLongBreak() {
        show(
            TimerUiState(
                mode = Mode.LONG_BREAK,
                showSkip = true,
                status = TimerStatus.LONG_BREAK,
                cycleWorkCount = 4,
            ),
            "en",
        )
        compose.onNodeWithText("Skip break").assertIsDisplayed()
    }

    @Test
    fun pluralSessions() {
        show(TimerUiState(), "pt-PT")
        listOf(0, 1, 2, 5).forEach { count ->
            applyLanguage("pt-PT")
            update(TimerUiState(todayCount = count))
            val pt = localizedContext("pt-PT")
            compose
                .onNodeWithText(
                    pt.resources.getQuantityString(R.plurals.session_word, count),
                    substring = false,
                ).assertExists()
            compose
                .onNode(
                    hasContentDescription(pt.resources.getQuantityString(R.plurals.session_count, count, count)),
                ).assertExists()

            applyLanguage("en")
            update(TimerUiState(todayCount = count))
            val en = localizedContext("en")
            compose
                .onNodeWithText(
                    en.resources.getQuantityString(R.plurals.session_word, count),
                    substring = false,
                ).assertExists()
        }
    }

    @Test
    fun steppersDisabledWhileRunning() {
        show(
            TimerUiState(
                isRunning = true,
                steppersEnabled = false,
                primary = PrimaryControl.PAUSE,
                status = TimerStatus.WORKING,
            ),
            "en",
        )
        compose.onNodeWithContentDescription("Decrease work").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Increase work").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Decrease break").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Increase break").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Decrease long break").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Increase long break").assertIsNotEnabled()
    }

    @Test
    fun soundToggle_descriptionFlips() {
        val state = mutableStateOf(TimerUiState(soundEnabled = true))
        applyLanguage("en")
        compose.setContent {
            TimerContent(
                state = state.value,
                reducedMotion = true,
                onAction = { action ->
                    if (action is TimerAction.SetSoundEnabled) {
                        state.value = state.value.copy(soundEnabled = action.enabled)
                    }
                },
                onFrame = {},
            )
        }
        compose.onNode(hasContentDescription("Turn completion sound off")).performClick()
        compose.onNode(hasContentDescription("Turn completion sound on")).assertIsOff()
    }

    @Test
    fun permissionBanner_shownWhenDenied_opensSettings() {
        show(TimerUiState(showNotificationBanner = true), "en")
        compose
            .onNodeWithText(
                "Notifications are off: you will not see the time or the end alert with the screen locked.",
            ).assertIsDisplayed()
        compose.onNodeWithText("Open settings").performClick()
        val intent = shadowOf(compose.activity).nextStartedActivity
        assertThat(intent.action).isEqualTo(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        assertThat(intent.getStringExtra(android.provider.Settings.EXTRA_APP_PACKAGE))
            .isEqualTo(compose.activity.packageName)
    }

    @Test
    fun notificationBanner_staysHiddenUntilThePermissionContractReturnsDenied() {
        val registry = HoldRegistry()
        val owner =
            object : ActivityResultRegistryOwner {
                override val activityResultRegistry: ActivityResultRegistry = registry
            }
        val state = mutableStateOf(TimerUiState(requestNotificationOnStart = true))
        val actions = mutableListOf<TimerAction>()
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                TimerContent(
                    state = state.value,
                    reducedMotion = true,
                    onAction = { action ->
                        actions += action
                        if (action == TimerAction.MarkNotificationAsked) {
                            state.value =
                                state.value.copy(
                                    requestNotificationOnStart = false,
                                    showNotificationBanner = true,
                                )
                        }
                    },
                    onFrame = {},
                )
            }
        }
        compose.onNodeWithText("Start").performClick()
        compose.onNodeWithText("Start").performClick()
        assertThat(actions.filterIsInstance<TimerAction.Start>()).isNotEmpty()
        assertThat(actions.filterIsInstance<TimerAction.MarkNotificationAsked>()).isEmpty()
        assertAbsent("Notifications are off: you will not see the time or the end alert with the screen locked.")
        assertThat(registry.codes).hasSize(1)

        registry.dispatchResult(registry.codes.single(), false)
        compose.waitForIdle()

        assertThat(actions.filterIsInstance<TimerAction.MarkNotificationAsked>()).hasSize(1)
        compose
            .onNodeWithText("Notifications are off: you will not see the time or the end alert with the screen locked.")
            .assertIsDisplayed()
    }

    @Test
    fun runningWindowTitle_usesTheStringResource() {
        show(
            TimerUiState(
                isRunning = true,
                status = TimerStatus.WORKING,
                timeText = "24:00",
                primary = PrimaryControl.PAUSE,
            ),
            "en",
        )
        assertThat(compose.activity.title.toString()).isEqualTo("Working · 24:00 — Lume")
    }

    @Test
    fun dismissControl_isACrossWithADescription() {
        show(TimerUiState(showNotificationBanner = true), "en")
        compose.onNodeWithText("×").assertIsDisplayed()
        compose.onNodeWithContentDescription("Dismiss").assertExists()
        assertAbsent("Dismiss")
    }

    @Test
    fun exactAlarmBanner_usesAShortActionLabel() {
        show(TimerUiState(showExactAlarmBanner = true), "en")
        compose.onNodeWithText("Alerts may be delayed. Allow exact alarms").assertIsDisplayed()
        compose.onNodeWithText("Allow exact alarms").assertIsDisplayed()
    }

    @Test
    fun titleUsesFrauncesAndBodyUsesOutfit() {
        show(TimerUiState(), "en")
        assertThat(fontFamily(compose.onNodeWithText("Lume"))).isEqualTo(com.marcopt74.damelume.ui.theme.LumeFonts.Fraunces)
        assertThat(fontFamily(compose.onNodeWithText("One thing at a time")))
            .isEqualTo(com.marcopt74.damelume.ui.theme.LumeFonts.Outfit)
        assertThat(com.marcopt74.damelume.ui.theme.LumeTypography.timer.fontFamily)
            .isEqualTo(com.marcopt74.damelume.ui.theme.LumeFonts.Outfit)
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun spaceKeyToggles() {
        val actions = mutableListOf<TimerAction>()
        show(TimerUiState(), "en") { actions += it }
        compose.onRoot().performKeyInput { keyDown(Key.Spacebar) }
        assertThat(actions).contains(TimerAction.Start)
    }

    @Test
    fun longBreakLabels_ptAndEn() {
        val running =
            TimerUiState(
                mode = Mode.LONG_BREAK,
                status = TimerStatus.LONG_RESTING,
                timeText = "15:00",
                showSkip = true,
                cycleWorkCount = 4,
                isRunning = true,
                primary = PrimaryControl.PAUSE,
            )
        show(running, "pt-PT")
        compose.onNodeWithContentDescription("Em pausa longa, 15:00 restantes").assertExists()
        assertAbsent("EM PAUSA LONGA")
        assertAbsent("15:00")

        applyLanguage("en")
        val paused =
            running.copy(
                isRunning = false,
                status = TimerStatus.LONG_BREAK_PAUSED,
                primary = PrimaryControl.RESUME,
                timeText = "14:00",
            )
        update(paused)
        compose.onNodeWithContentDescription("Long break paused, 14:00 left").assertExists()
        assertAbsent("LONG BREAK PAUSED")

        update(paused.copy(status = TimerStatus.LONG_BREAK, primary = PrimaryControl.START, timeText = "15:00"))
        compose.onNodeWithContentDescription("Long break, 15:00 left").assertExists()
        assertAbsent("LONG BREAK")
    }

    private fun assertAbsent(text: String) {
        assertThat(
            compose.onAllNodes(hasText(text, substring = false)).fetchSemanticsNodes(false),
        ).isEmpty()
    }

    private val screenState = mutableStateOf(TimerUiState())
    private val screenContext = mutableStateOf<Context?>(null)

    private fun update(state: TimerUiState) {
        screenState.value = state
        compose.waitForIdle()
    }

    private var contentReady = false
    private var screenAction: (TimerAction) -> Unit = {}

    private fun show(
        state: TimerUiState,
        language: String,
        onAction: (TimerAction) -> Unit = {},
    ) {
        screenContext.value = localizedContext(language)
        screenAction = onAction
        screenState.value = state
        if (!contentReady) {
            contentReady = true
            compose.setContent {
                CompositionLocalProvider(
                    LocalContext provides (screenContext.value ?: compose.activity),
                    LocalActivityResultRegistryOwner provides compose.activity,
                ) {
                    TimerContent(
                        state = screenState.value,
                        reducedMotion = true,
                        onAction = { screenAction(it) },
                        onFrame = {},
                    )
                }
            }
        }
    }

    private fun applyLanguage(language: String) {
        screenContext.value = localizedContext(language)
        compose.waitForIdle()
    }

    private fun context(language: String): Context = localizedContext(language)

    private fun localizedContext(language: String): Context {
        val base = compose.activity
        if (language.startsWith("en")) return base
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocales(LocaleList(Locale.forLanguageTag(language)))
        return base.createConfigurationContext(configuration)
    }

    private fun fontFamily(node: SemanticsNodeInteraction): FontFamily? {
        val results = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { callback ->
            callback(results)
        }
        return results
            .first()
            .layoutInput.style.fontFamily
    }
}

private class HoldRegistry : ActivityResultRegistry() {
    val codes = mutableListOf<Int>()

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        codes += requestCode
    }
}
