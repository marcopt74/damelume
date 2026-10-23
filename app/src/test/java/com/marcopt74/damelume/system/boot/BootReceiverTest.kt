package com.marcopt74.damelume.system.boot

import android.content.Context
import android.content.Intent
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
class BootReceiverTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                LumeDatabase::class.java,
            ).allowMainThreadQueries()
            .build()

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun reschedulesFuture_completesPast() =
        runTest {
            val futureEnds = 50_000L
            val clock = MutableClock(now = 10_000)
            val preferences = preferences("boot-future")
            preferences.writeTimer(
                TimerState(mode = Mode.WORK, isRunning = true, endsAtEpochMs = futureEnds, remainingMs = 40_000, cycleWorkCount = 1),
            )
            val alarm = RecordingAlarmScheduler()
            val handler = BootHandler(repository(preferences, clock, alarm, RecordingNotifier()))

            handler.onBroadcast(Intent.ACTION_BOOT_COMPLETED)

            assertThat(alarm.scheduled).contains(futureEnds)
            assertThat(preferences.current().timer.isRunning).isTrue()

            val pastEnds = 1_000L
            val pastClock = MutableClock(now = pastEnds + 120_000)
            val pastPrefs = preferences("boot-past")
            pastPrefs.writeTimer(
                TimerState(mode = Mode.WORK, isRunning = true, endsAtEpochMs = pastEnds, remainingMs = 1),
            )
            val pastNotifier = RecordingNotifier()
            BootHandler(repository(pastPrefs, pastClock, RecordingAlarmScheduler(), pastNotifier))
                .onBroadcast(Intent.ACTION_MY_PACKAGE_REPLACED)

            assertThat(pastPrefs.current().timer.mode).isEqualTo(Mode.BREAK)
            assertThat(pastPrefs.current().timer.isRunning).isFalse()
            assertThat(pastNotifier.completions).isNotEmpty()
            assertThat(pastNotifier.completions.last().soundEnabled).isFalse()
        }

    @Test
    fun longBreak_reschedules() =
        runTest {
            val endsAt = 80_000L
            val clock = MutableClock(now = 20_000)
            val preferences = preferences("boot-long")
            preferences.writeTimer(
                TimerState(
                    mode = Mode.LONG_BREAK,
                    isRunning = true,
                    endsAtEpochMs = endsAt,
                    remainingMs = 60_000,
                    cycleWorkCount = 4,
                ),
            )
            val alarm = RecordingAlarmScheduler()
            BootHandler(repository(preferences, clock, alarm, RecordingNotifier()))
                .onBroadcast(Intent.ACTION_BOOT_COMPLETED)

            val timer = preferences.current().timer
            assertThat(timer.mode).isEqualTo(Mode.LONG_BREAK)
            assertThat(timer.cycleWorkCount).isEqualTo(4)
            assertThat(timer.isRunning).isTrue()
            assertThat(alarm.scheduled).contains(endsAt)
        }

    private fun preferences(name: String): LumePreferencesDataSource {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.filesDir, "$name.preferences_pb")
        return LumePreferencesDataSource(PreferenceDataStoreFactory.create { file })
    }

    private fun repository(
        preferences: LumePreferencesDataSource,
        clock: MutableClock,
        alarm: RecordingAlarmScheduler,
        notifier: RecordingNotifier,
    ) = TimerRepository(
        preferences = preferences,
        dao = database.completedSessionDao(),
        clock = clock,
        alarmScheduler = alarm,
        notifier = notifier,
        chimePlayer = RecordingChime(),
        foregroundTracker = FakeForeground(foreground = false),
        localeApplicator = FakeLocaleApplicator(),
    )
}
