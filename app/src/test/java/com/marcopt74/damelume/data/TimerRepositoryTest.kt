package com.marcopt74.damelume.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.data.db.CompletedSessionEntity
import com.marcopt74.damelume.data.db.LumeDatabase
import com.marcopt74.damelume.data.prefs.LumePreferencesDataSource
import com.marcopt74.damelume.domain.model.Mode
import com.marcopt74.damelume.domain.model.TimerAction
import com.marcopt74.damelume.domain.model.TimerState
import com.marcopt74.damelume.testutil.FakeLocaleApplicator
import com.marcopt74.damelume.testutil.MutableClock
import com.marcopt74.damelume.testutil.RecordingAlarmScheduler
import com.marcopt74.damelume.testutil.RecordingChime
import com.marcopt74.damelume.testutil.RecordingNotifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class TimerRepositoryTest {
    private lateinit var database: LumeDatabase
    private lateinit var preferences: LumePreferencesDataSource
    private lateinit var clock: MutableClock
    private lateinit var alarm: RecordingAlarmScheduler
    private lateinit var notifier: RecordingNotifier
    private lateinit var chime: RecordingChime
    private lateinit var repository: TimerRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, LumeDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val file = File(context.filesDir, "repo-${System.nanoTime()}.preferences_pb")
        preferences = LumePreferencesDataSource(PreferenceDataStoreFactory.create { file })
        clock = MutableClock()
        alarm = RecordingAlarmScheduler()
        notifier = RecordingNotifier()
        chime = RecordingChime()
        repository = repository()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun resetSessions_keepsHistory() =
        runTest {
            database.completedSessionDao().insert(
                CompletedSessionEntity(
                    mode = "WORK",
                    plannedMs = 25 * 60_000L,
                    endedAtEpochMs = 10,
                    localDate = clock.today().toString(),
                    lateCompletion = false,
                ),
            )
            preferences.writeTimer(TimerState(mode = Mode.BREAK, isRunning = true))

            repository.dispatch(TimerAction.ResetSessions)

            assertThat(database.completedSessionDao().countAll()).isEqualTo(1)
            assertThat(repository.todayCount.first()).isEqualTo(0)
            assertThat(preferences.current().timer.mode).isEqualTo(Mode.BREAK)
            assertThat(preferences.current().timer.isRunning).isTrue()
        }

    @Test
    fun completeInForeground_playsChimeOnlyIfEnabled() =
        runTest {
            repository.dispatch(TimerAction.Start)
            val endsAt = preferences.current().timer.endsAtEpochMs!!
            clock.now = endsAt

            repository.dispatch(TimerAction.Complete(expectedEndsAt = endsAt))

            assertThat(chime.plays).isEqualTo(1)
            assertThat(notifier.completions).hasSize(1)
            assertThat(preferences.current().timer.isRunning).isTrue()
            assertThat(preferences.current().timer.mode).isEqualTo(Mode.BREAK)

            preferences.setSoundEnabled(false)
            val secondEnds = preferences.current().timer.endsAtEpochMs!!
            clock.now = secondEnds
            repository.dispatch(TimerAction.Complete(expectedEndsAt = secondEnds))

            assertThat(chime.plays).isEqualTo(1)
            assertThat(notifier.completions).hasSize(2)
            assertThat(preferences.current().timer.mode).isEqualTo(Mode.WORK)
            assertThat(preferences.current().timer.isRunning).isTrue()
        }

    @Test
    fun completeInBackground_playsChimeAndShowsSilentNotice() =
        runTest {
            repository.dispatch(TimerAction.Start)
            val endsAt = preferences.current().timer.endsAtEpochMs!!
            clock.now = endsAt

            repository.dispatch(TimerAction.Complete(expectedEndsAt = endsAt))

            assertThat(chime.plays).isEqualTo(1)
            assertThat(notifier.completions).hasSize(1)
            assertThat(notifier.completions.single().soundEnabled).isFalse()

            preferences.setSoundEnabled(false)
            repository.dispatch(TimerAction.Start)
            val secondEnds = preferences.current().timer.endsAtEpochMs!!
            clock.now = secondEnds
            repository.dispatch(TimerAction.Complete(expectedEndsAt = secondEnds))

            assertThat(chime.plays).isEqualTo(1)
            assertThat(notifier.completions).hasSize(2)
            assertThat(notifier.completions.last().soundEnabled).isFalse()
        }

    @Test
    fun pausedSurvivesRecreation() =
        runTest {
            repository.dispatch(TimerAction.Start)
            clock.now += 5_000
            repository.dispatch(TimerAction.Pause)
            val paused = preferences.current().timer

            val restoredAlarm = RecordingAlarmScheduler()
            val restored = repository(alarm = restoredAlarm)
            restored.reconcileOnLaunch()

            val again = preferences.current().timer
            assertThat(again.isRunning).isFalse()
            assertThat(again.remainingMs).isEqualTo(paused.remainingMs)
            assertThat(again.endsAtEpochMs).isNull()
            assertThat(restoredAlarm.scheduled).isEmpty()
        }

    @Test
    fun skipBreak_cancelsAlarmAndOngoing() =
        runTest {
            val endsAt = clock.now + 60_000
            preferences.writeTimer(
                TimerState(
                    mode = Mode.BREAK,
                    isRunning = true,
                    endsAtEpochMs = endsAt,
                    remainingMs = 60_000,
                ),
            )

            repository.dispatch(TimerAction.SkipBreak)

            val timer = preferences.current().timer
            assertThat(timer.mode).isEqualTo(Mode.WORK)
            assertThat(timer.isRunning).isFalse()
            assertThat(alarm.cancelled).contains(endsAt)
            assertThat(notifier.cancelCount).isAtLeast(1)
            assertThat(notifier.ongoing).isNull()
        }

    private fun repository(alarm: RecordingAlarmScheduler = this.alarm): TimerRepository =
        TimerRepository(
            preferences = preferences,
            dao = database.completedSessionDao(),
            clock = clock,
            alarmScheduler = alarm,
            notifier = notifier,
            chimePlayer = chime,
            localeApplicator = FakeLocaleApplicator(),
        )
}
