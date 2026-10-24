package com.marcopt74.damelume.ui.timer

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.R
import com.marcopt74.damelume.domain.model.Mode
import com.marcopt74.damelume.domain.model.TimerAction
import com.marcopt74.damelume.testing.TimerTestActivity
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
        compose.onNodeWithText("25:00").assertIsDisplayed()
        compose.onNodeWithText(pt.getString(R.string.work).uppercase()).assertIsDisplayed()
        compose.onNodeWithText(pt.getString(R.string.start)).assertIsDisplayed()
        compose.onNodeWithText("0").assertIsDisplayed()
        compose
            .onNodeWithText(pt.resources.getQuantityString(R.plurals.session_word, 0), substring = false)
            .assertIsDisplayed()
        compose.onNodeWithText(pt.getString(R.string.reset_sessions)).assertIsNotEnabled()
        compose.onNode(hasContentDescription(pt.getString(R.string.sound_off))).assertIsOn()
        compose.onNodeWithText("25").assertIsDisplayed()
        compose.onNodeWithText("5").assertIsDisplayed()
        assertThat(
            compose.onAllNodes(hasText(pt.getString(R.string.minutes))).fetchSemanticsNodes().size,
        ).isEqualTo(2)
        assertAbsent("15")
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
        compose.onNodeWithContentDescription("Edit work minutes").assertIsNotEnabled()
    }

    @Test
    fun typedMinutes_commitPositiveInteger() {
        val actions = mutableListOf<TimerAction>()
        show(TimerUiState(), "en") { actions += it }
        compose.onNodeWithContentDescription("Edit work minutes").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("40")
        compose.onNode(hasSetTextAction()).performImeAction()
        assertThat(actions).contains(TimerAction.SetWorkMinutes(40))
    }

    @Test
    fun typedZero_isIgnored() {
        val actions = mutableListOf<TimerAction>()
        show(TimerUiState(), "en") { actions += it }
        compose.onNodeWithContentDescription("Edit break minutes").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("0")
        compose.onNode(hasSetTextAction()).performImeAction()
        assertThat(actions.filterIsInstance<TimerAction.SetBreakMinutes>()).isEmpty()
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
    fun stateSurvivesRecreation() {
        val scenario = ActivityScenario.launch(TimerTestActivity::class.java)
        scenario.onActivity { activity ->
            activity.model.onAction(TimerAction.Start)
            assertThat(activity.model.state.isRunning).isTrue()
        }
        scenario.recreate()
        scenario.onActivity { activity ->
            assertThat(activity.model.state.isRunning).isTrue()
            assertThat(activity.model.state.primary).isEqualTo(PrimaryControl.PAUSE)
            assertThat(activity.model.state.status).isEqualTo(TimerStatus.WORKING)
        }
        scenario.close()
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
    fun breakLabels_ptAndEn() {
        val running =
            TimerUiState(
                mode = Mode.BREAK,
                status = TimerStatus.RESTING,
                timeText = "05:00",
                showSkip = true,
                isRunning = true,
                primary = PrimaryControl.PAUSE,
            )
        show(running, "pt-PT")
        compose.onNodeWithText("A DESCANSAR").assertIsDisplayed()
        compose.onNodeWithContentDescription("A descansar, 05:00 restantes").assertExists()

        applyLanguage("en")
        val paused =
            running.copy(
                isRunning = false,
                status = TimerStatus.BREAK_PAUSED,
                primary = PrimaryControl.RESUME,
                timeText = "04:00",
            )
        update(paused)
        compose.onNodeWithText("BREAK PAUSED").assertIsDisplayed()
        compose.onNodeWithContentDescription("Break paused, 04:00 left").assertExists()
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
}
