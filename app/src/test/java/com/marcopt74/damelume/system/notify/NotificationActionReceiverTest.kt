package com.marcopt74.damelume.system.notify

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.data.TimerRepository
import com.marcopt74.damelume.data.db.LumeDatabase
import com.marcopt74.damelume.data.prefs.LumePreferencesDataSource
import com.marcopt74.damelume.domain.model.Mode
import com.marcopt74.damelume.domain.model.TimerState
import com.marcopt74.damelume.testutil.FakeForeground
import com.marcopt74.damelume.testutil.FakeLocaleApplicator
import com.marcopt74.damelume.testutil.MutableClock
import com.marcopt74.damelume.testutil.RecordingAlarmScheduler
import com.marcopt74.damelume.testutil.RecordingChime
import com.marcopt74.damelume.testutil.RecordingNotifier
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class NotificationActionReceiverTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                LumeDatabase::class.java,
            ).allowMainThreadQueries()
            .build()
    private val clock = MutableClock()
    private val alarm = RecordingAlarmScheduler()
    private val notifier = RecordingNotifier()

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun pause_resume_reset_skip() =
        runTest {
            val preferences = preferences()
            val handler = NotificationActionHandler(repository(preferences))
            val endsAt = clock.now + 60_000
            preferences.writeTimer(
                TimerState(mode = Mode.BREAK, isRunning = true, endsAtEpochMs = endsAt, remainingMs = 60_000),
            )

            handler.handle(NotificationActionReceiver.ACTION_PAUSE)
            var timer = preferences.current().timer
            assertThat(timer.isRunning).isFalse()
            assertThat(alarm.cancelled).contains(endsAt)

            handler.handle(NotificationActionReceiver.ACTION_RESUME)
            timer = preferences.current().timer
            assertThat(timer.isRunning).isTrue()
            assertThat(alarm.scheduled).isNotEmpty()

            handler.handle(NotificationActionReceiver.ACTION_RESET)
            timer = preferences.current().timer
            assertThat(timer.isRunning).isFalse()
            assertThat(timer.mode).isEqualTo(Mode.BREAK)
            assertThat(timer.remainingMs).isEqualTo(timer.breakMinutes * 60_000L)

            preferences.writeTimer(
                TimerState(mode = Mode.BREAK, isRunning = true, endsAtEpochMs = clock.now + 1_000),
            )
            handler.handle(NotificationActionReceiver.ACTION_SKIP)
            timer = preferences.current().timer
            assertThat(timer.mode).isEqualTo(Mode.WORK)
            assertThat(timer.isRunning).isFalse()
            assertThat(notifier.cancelCount).isAtLeast(1)
        }

    private fun preferences(): LumePreferencesDataSource {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.filesDir, "actions.preferences_pb")
        return LumePreferencesDataSource(PreferenceDataStoreFactory.create { file })
    }

    private fun repository(preferences: LumePreferencesDataSource) =
        TimerRepository(
            preferences = preferences,
            dao = database.completedSessionDao(),
            clock = clock,
            alarmScheduler = alarm,
            notifier = notifier,
            chimePlayer = RecordingChime(),
            foregroundTracker = FakeForeground(foreground = true),
            localeApplicator = FakeLocaleApplicator(),
        )
}
