package com.marcopt74.damelume.ui.timer

import android.content.Context
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.R
import com.marcopt74.damelume.data.TimerRepository
import com.marcopt74.damelume.data.db.LumeDatabase
import com.marcopt74.damelume.data.prefs.LumePreferencesDataSource
import com.marcopt74.damelume.domain.model.TimerAction
import com.marcopt74.damelume.system.alarm.ExactAlarmCapability
import com.marcopt74.damelume.system.notify.DevicePermissions
import com.marcopt74.damelume.testing.TimerRotationHost
import com.marcopt74.damelume.testing.TimerTestActivity
import com.marcopt74.damelume.testutil.FakeForeground
import com.marcopt74.damelume.testutil.FakeLocaleApplicator
import com.marcopt74.damelume.testutil.MutableClock
import com.marcopt74.damelume.testutil.RecordingAlarmScheduler
import com.marcopt74.damelume.testutil.RecordingChime
import com.marcopt74.damelume.testutil.RecordingNotifier
import com.marcopt74.damelume.testutil.quietAppScope
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32], qualifiers = "w480dp-h2400dp")
class TimerRotationTest {
    private val environment = RotationEnvironment()
    private val compose = createAndroidComposeRule<TimerTestActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(environment).around(compose)

    @Test
    fun rotationAndLocaleKeepEndsAtAndTheDisplayedTime() {
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(100)
        shadowOf(Looper.getMainLooper()).idle()
        compose.runOnUiThread {
            compose.activity.viewModel.onAction(TimerAction.Start)
        }
        val endsAt = waitForEndsAt()
        compose.runOnUiThread {
            environment.clock.now = endsAt - 60_000
            compose.activity.viewModel.onFrame()
        }
        waitForDescription("Working, 01:00 left")

        compose.activityRule.scenario.recreate()
        waitUntil { compose.activity.viewModel.uiState.value.endsAtEpochMs == endsAt }
        compose.runOnUiThread {
            val state = compose.activity.viewModel.uiState.value
            assertThat(state.endsAtEpochMs).isEqualTo(endsAt)
            assertThat(state.timeText).isEqualTo("01:00")
        }
        waitForDescription("Working, 01:00 left")

        compose.runOnUiThread {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("pt-PT"))
        }
        compose.activityRule.scenario.recreate()
        compose.mainClock.autoAdvance = false
        waitUntil { compose.activity.getString(R.string.working) == "A trabalhar" }
        waitForDescription("A trabalhar, 01:00 restantes")
        compose.runOnUiThread {
            val state = compose.activity.viewModel.uiState.value
            assertThat(state.endsAtEpochMs).isEqualTo(endsAt)
            assertThat(state.timeText).isEqualTo("01:00")
        }
    }

    private fun waitForEndsAt(): Long {
        var endsAt = 0L
        waitUntil {
            val current = compose.activity.viewModel.uiState.value.endsAtEpochMs
            if (current != null && compose.activity.viewModel.uiState.value.isRunning) {
                endsAt = current
                true
            } else {
                false
            }
        }
        return endsAt
    }

    private fun waitForDescription(description: String) {
        waitUntil {
            compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitUntil(predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!predicate()) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.mainClock.advanceTimeBy(16)
            if (System.currentTimeMillis() > deadline) {
                error("Timed out. state=${compose.activity.viewModel.uiState.value}")
            }
            Thread.sleep(20)
        }
    }
}

private class RotationEnvironment : TestWatcher() {
    lateinit var clock: MutableClock
    private var database: LumeDatabase? = null

    override fun starting(description: Description) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database =
            Room
                .inMemoryDatabaseBuilder(context, LumeDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        this.database = database
        val preferences =
            LumePreferencesDataSource(
                PreferenceDataStoreFactory.create {
                    File(context.filesDir, "rotation-${System.nanoTime()}.preferences_pb")
                },
            )
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        clock = MutableClock()
        TimerRotationHost.repository =
            TimerRepository(
                preferences = preferences,
                dao = database.completedSessionDao(),
                clock = clock,
                alarmScheduler = RecordingAlarmScheduler(),
                notifier = RecordingNotifier(),
                chimePlayer = RecordingChime(),
                foregroundTracker = FakeForeground(foreground = true),
                localeApplicator = FakeLocaleApplicator(),
                scope = quietAppScope(),
            )
        TimerRotationHost.clock = clock
        TimerRotationHost.foreground = FakeForeground(foreground = true)
        TimerRotationHost.permissions =
            object : DevicePermissions {
                override fun sdkInt(): Int = 30

                override fun notificationsGranted(): Boolean = true

                override fun exactAlarmCapability(): ExactAlarmCapability = ExactAlarmCapability(30, true)
            }
    }

    override fun finished(description: Description) {
        database?.close()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }
}
