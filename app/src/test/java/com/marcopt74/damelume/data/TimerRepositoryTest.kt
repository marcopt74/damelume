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
import com.marcopt74.damelume.testutil.FakeForeground
import com.marcopt74.damelume.testutil.FakeLocaleApplicator
import com.marcopt74.damelume.testutil.MutableClock
import com.marcopt74.damelume.testutil.RecordingAlarmScheduler
import com.marcopt74.damelume.testutil.RecordingChime
import com.marcopt74.damelume.testutil.RecordingNotifier
import com.marcopt74.damelume.testutil.quietAppScope
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
    private lateinit var foreground: FakeForeground
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
        foreground = FakeForeground(foreground = true)
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
            preferences.writeTimer(TimerState(cycleWorkCount = 2))

            repository.dispatch(TimerAction.ResetSessions)

            assertThat(database.completedSessionDao().countAll()).isEqualTo(1)
            assertThat(repository.todayCount.first()).isEqualTo(0)
            assertThat(preferences.current().timer.cycleWorkCount).isEqualTo(0)
        }

    @Test
    fun completeInForeground_playsChimeOnlyIfEnabled() =
        runTest {
            repository.dispatch(TimerAction.Start)
            val endsAt = preferences.current().timer.endsAtEpochMs!!
            clock.now = endsAt
            foreground.foreground = true

            repository.dispatch(TimerAction.Complete(expectedEndsAt = endsAt))

            assertThat(chime.plays).isEqualTo(1)
            assertThat(notifier.completions).isEmpty()

            preferences.setSoundEnabled(false)
            repository.dispatch(TimerAction.Start)
            val secondEnds = preferences.current().timer.endsAtEpochMs!!
            clock.now = secondEnds
            repository.dispatch(TimerAction.Complete(expectedEndsAt = secondEnds))

            assertThat(chime.plays).isEqualTo(1)
            assertThat(notifier.completions).isEmpty()
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
    fun skipLongBreak_cancelsAlarmAndOngoing() =
        runTest {
            val endsAt = clock.now + 60_000
            preferences.writeTimer(
                TimerState(
                    mode = Mode.LONG_BREAK,
                    isRunning = true,
                    endsAtEpochMs = endsAt,
                    remainingMs = 60_000,
                    cycleWorkCount = 4,
                ),
            )

            repository.dispatch(TimerAction.SkipBreak)

            val timer = preferences.current().timer
            assertThat(timer.mode).isEqualTo(Mode.WORK)
            assertThat(timer.cycleWorkCount).isEqualTo(0)
            assertThat(timer.isRunning).isFalse()
            assertThat(alarm.cancelled).contains(endsAt)
            assertThat(notifier.cancelCount).isAtLeast(1)
            assertThat(notifier.ongoing).isNull()
        }

    @Test
    fun resetSessions_inLongBreak_cancelsAlarm() =
        runTest {
            val endsAt = clock.now + 30_000
            preferences.writeTimer(
                TimerState(
                    mode = Mode.LONG_BREAK,
                    isRunning = true,
                    endsAtEpochMs = endsAt,
                    remainingMs = 30_000,
                    cycleWorkCount = 4,
                    workMinutes = 25,
                ),
            )

            repository.dispatch(TimerAction.ResetSessions)

            val timer = preferences.current().timer
            assertThat(timer.mode).isEqualTo(Mode.WORK)
            assertThat(timer.isRunning).isFalse()
            assertThat(timer.cycleWorkCount).isEqualTo(0)
            assertThat(timer.remainingMs).isEqualTo(25 * 60_000L)
            assertThat(alarm.cancelled).contains(endsAt)
            assertThat(notifier.cancelCount).isAtLeast(1)
        }

    @Test
    fun rescheduleRunningAlarm_rewritesTheSameEndsAt_andSkipsIdle() =
        runTest {
            repository.dispatch(TimerAction.Start)
            val endsAt = preferences.current().timer.endsAtEpochMs
            assertThat(endsAt).isNotNull()
            alarm.scheduled.clear()
            alarm.cancelled.clear()

            repository.rescheduleRunningAlarm()

            assertThat(alarm.cancelled).containsExactly(endsAt)
            assertThat(alarm.scheduled).containsExactly(endsAt)

            repository.dispatch(TimerAction.Pause)
            alarm.scheduled.clear()
            alarm.cancelled.clear()
            repository.rescheduleRunningAlarm()

            assertThat(alarm.scheduled).isEmpty()
        }

    @Test
    fun failedInsert_doesNotAdvanceTheTimer_andRetryDoesNotDuplicate() =
        runTest {
            val flaky = FlakyDao(database.completedSessionDao())
            val flakyRepository =
                TimerRepository(
                    preferences = preferences,
                    dao = flaky,
                    clock = clock,
                    alarmScheduler = alarm,
                    notifier = notifier,
                    chimePlayer = chime,
                    foregroundTracker = foreground,
                    localeApplicator = FakeLocaleApplicator(),
                    scope = quietAppScope(),
                )
            flakyRepository.dispatch(TimerAction.Start)
            val endsAt = preferences.current().timer.endsAtEpochMs!!
            clock.now = endsAt
            flaky.failInserts = 1

            flakyRepository.dispatch(TimerAction.Complete(expectedEndsAt = endsAt))

            assertThat(preferences.current().timer.isRunning).isTrue()
            assertThat(preferences.current().timer.mode).isEqualTo(Mode.WORK)
            assertThat(database.completedSessionDao().countAll()).isEqualTo(0)

            flakyRepository.dispatch(TimerAction.Complete(expectedEndsAt = endsAt))

            assertThat(preferences.current().timer.isRunning).isFalse()
            assertThat(preferences.current().timer.mode).isEqualTo(Mode.BREAK)
            assertThat(database.completedSessionDao().countAll()).isEqualTo(1)
            val stored = database.completedSessionDao().all().single()
            database.completedSessionDao().insert(stored.copy(id = 0))
            assertThat(database.completedSessionDao().countAll()).isEqualTo(1)
        }

    @Test
    fun reconcileYesterdayWork_countsAsToday() =
        runTest {
            val zone = clock.zone()
            val endsAt =
                java.time.ZonedDateTime
                    .of(2026, 10, 22, 23, 0, 0, 0, zone)
                    .toInstant()
                    .toEpochMilli()
            val morning =
                java.time.ZonedDateTime
                    .of(2026, 10, 23, 9, 0, 0, 0, zone)
                    .toInstant()
                    .toEpochMilli()
            preferences.writeTimer(
                TimerState(mode = Mode.WORK, isRunning = true, endsAtEpochMs = endsAt, remainingMs = 1_000, cycleWorkCount = 0),
            )
            clock.now = morning
            repository.refreshToday()

            repository.reconcileOnLaunch()

            val rows = database.completedSessionDao().all()
            assertThat(rows).hasSize(1)
            assertThat(rows.single().endedAtEpochMs).isEqualTo(endsAt)
            assertThat(rows.single().localDate).isEqualTo("2026-10-23")
            assertThat(rows.single().mode).isEqualTo("WORK")
            assertThat(repository.todayCount.first()).isEqualTo(1)
        }

    @Test
    fun refreshToday_rereadsTheDateWithoutAPreferenceWrite() =
        runTest {
            val zone = clock.zone()
            val noon =
                java.time.ZonedDateTime
                    .of(2026, 10, 22, 12, 0, 0, 0, zone)
                    .toInstant()
                    .toEpochMilli()
            clock.now = noon
            repository.refreshToday()
            database.completedSessionDao().insert(
                CompletedSessionEntity(
                    mode = "WORK",
                    plannedMs = 25 * 60_000L,
                    endedAtEpochMs = noon,
                    localDate = clock.today().toString(),
                    lateCompletion = false,
                ),
            )
            assertThat(repository.todayCount.first()).isEqualTo(1)

            clock.now = noon + 24 * 60 * 60 * 1000
            assertThat(repository.todayCount.first()).isEqualTo(1)

            repository.refreshToday()
            assertThat(repository.todayCount.first()).isEqualTo(0)
        }

    private fun repository(alarm: RecordingAlarmScheduler = this.alarm): TimerRepository =
        TimerRepository(
            preferences = preferences,
            dao = database.completedSessionDao(),
            clock = clock,
            alarmScheduler = alarm,
            notifier = notifier,
            chimePlayer = chime,
            foregroundTracker = foreground,
            localeApplicator = FakeLocaleApplicator(),
            scope = quietAppScope(),
        )
}

private class FlakyDao(
    private val delegate: com.marcopt74.damelume.data.db.CompletedSessionDao,
) : com.marcopt74.damelume.data.db.CompletedSessionDao {
    var failInserts: Int = 0

    override suspend fun insert(entity: com.marcopt74.damelume.data.db.CompletedSessionEntity): Long {
        if (failInserts > 0) {
            failInserts -= 1
            error("disk full")
        }
        return delegate.insert(entity)
    }

    override fun todayCount(
        today: String,
        sessionsResetAt: Long,
    ) = delegate.todayCount(today, sessionsResetAt)

    override suspend fun countAll(): Int = delegate.countAll()

    override suspend fun all() = delegate.all()
}
