package com.marcopt74.damelume.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.data.db.CompletedSessionEntity
import com.marcopt74.damelume.data.db.LumeDatabase
import com.marcopt74.damelume.data.prefs.LumePreferencesDataSource
import com.marcopt74.damelume.domain.Clock
import com.marcopt74.damelume.testutil.FakeForeground
import com.marcopt74.damelume.testutil.FakeLocaleApplicator
import com.marcopt74.damelume.testutil.RecordingAlarmScheduler
import com.marcopt74.damelume.testutil.RecordingChime
import com.marcopt74.damelume.testutil.RecordingNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class TodayCountClockTest {
    private var database: LumeDatabase? = null
    private var scope: CoroutineScope? = null

    @After
    fun tearDown() {
        scope?.cancel()
        database?.close()
    }

    @Test
    fun todayCount_dropsWhenTheClockCrossesMidnightWithoutAPreferenceWrite() =
        runTest {
            val scheduler = TestCoroutineScheduler()
            val dispatcher = StandardTestDispatcher(scheduler)
            val appScope = CoroutineScope(SupervisorJob() + dispatcher)
            scope = appScope
            val start =
                Instant
                    .parse("2026-10-22T23:59:00Z")
                    .toEpochMilli()
            val clock =
                object : Clock {
                    override fun nowMillis(): Long = start + scheduler.currentTime

                    override fun zone() = ZoneOffset.UTC
                }
            val context = ApplicationProvider.getApplicationContext<Context>()
            val db =
                Room
                    .inMemoryDatabaseBuilder(context, LumeDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()
            database = db
            db.completedSessionDao().insert(
                CompletedSessionEntity(
                    mode = "WORK",
                    plannedMs = 25 * 60_000L,
                    endedAtEpochMs = start,
                    localDate = "2026-10-22",
                    lateCompletion = false,
                ),
            )
            val preferences =
                LumePreferencesDataSource(
                    PreferenceDataStoreFactory.create {
                        File(context.filesDir, "midnight-${System.nanoTime()}.preferences_pb")
                    },
                )
            val repository =
                TimerRepository(
                    preferences = preferences,
                    dao = db.completedSessionDao(),
                    clock = clock,
                    alarmScheduler = RecordingAlarmScheduler(),
                    notifier = RecordingNotifier(),
                    chimePlayer = RecordingChime(),
                    foregroundTracker = FakeForeground(),
                    localeApplicator = FakeLocaleApplicator(),
                    scope = appScope,
                )

            repository.todayCount.test {
                assertThat(awaitItem()).isEqualTo(1)
                scheduler.advanceTimeBy(61_000)
                assertThat(awaitItem()).isEqualTo(0)
                cancelAndIgnoreRemainingEvents()
            }
            appScope.cancel()
        }
}
