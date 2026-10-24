package com.marcopt74.damelume.system.alarm

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
class TimerAlarmReceiverTest {
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
    fun completesOnce_idempotentOnStaleAlarm() =
        runTest {
            val clock = MutableClock(now = 5_000)
            val preferences = preferences("alarm-once")
            val endsAt = 5_000L
            preferences.writeTimer(
                TimerState(mode = Mode.WORK, isRunning = true, endsAtEpochMs = endsAt, remainingMs = 1),
            )
            val repository = repository(preferences, clock)
            val handler = TimerAlarmHandler(repository)

            handler.onAlarm(endsAt)
            handler.onAlarm(endsAt)

            val timer = preferences.current().timer
            assertThat(timer.mode).isEqualTo(Mode.BREAK)
            assertThat(timer.isRunning).isTrue()
            assertThat(timer.endsAtEpochMs).isEqualTo(endsAt + 5 * 60_000L)
            assertThat(database.completedSessionDao().countAll()).isEqualTo(1)
        }

    @Test
    fun fourthWork_whileDead_entersBreak() =
        runTest {
            val endsAt = 9_000L
            val clock = MutableClock(now = endsAt)
            val preferences = preferences("alarm-fourth")
            preferences.writeTimer(
                TimerState(
                    mode = Mode.WORK,
                    isRunning = true,
                    endsAtEpochMs = endsAt,
                    remainingMs = 1,
                ),
            )
            val handler = TimerAlarmHandler(repository(preferences, clock))

            handler.onAlarm(endsAt)

            val timer = preferences.current().timer
            assertThat(timer.mode).isEqualTo(Mode.BREAK)
            assertThat(timer.isRunning).isTrue()
        }

    private fun preferences(name: String): LumePreferencesDataSource {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.filesDir, "$name.preferences_pb")
        return LumePreferencesDataSource(PreferenceDataStoreFactory.create { file })
    }

    private fun repository(
        preferences: LumePreferencesDataSource,
        clock: MutableClock,
    ) = TimerRepository(
        preferences = preferences,
        dao = database.completedSessionDao(),
        clock = clock,
        alarmScheduler = RecordingAlarmScheduler(),
        notifier = RecordingNotifier(),
        chimePlayer = RecordingChime(),
        localeApplicator = FakeLocaleApplicator(),
    )
}
