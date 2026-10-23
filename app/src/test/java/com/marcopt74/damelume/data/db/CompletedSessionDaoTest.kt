package com.marcopt74.damelume.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CompletedSessionDaoTest {
    private lateinit var database: LumeDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, LumeDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun todayCount_respectsResetAndDate() =
        runTest {
            val dao = database.completedSessionDao()
            dao.insert(session(mode = "WORK", endedAt = 100, date = "2026-10-22"))
            dao.insert(session(mode = "WORK", endedAt = 50, date = "2026-10-23"))
            dao.insert(session(mode = "WORK", endedAt = 200, date = "2026-10-23"))
            dao.insert(session(mode = "BREAK", endedAt = 300, date = "2026-10-23"))
            dao.insert(session(mode = "LONG_BREAK", endedAt = 400, date = "2026-10-23"))

            val count = dao.todayCount(today = "2026-10-23", sessionsResetAt = 100).first()

            assertThat(count).isEqualTo(1)
            assertThat(dao.countAll()).isEqualTo(5)
        }

    private fun session(
        mode: String,
        endedAt: Long,
        date: String,
    ) = CompletedSessionEntity(
        mode = mode,
        plannedMs = 25 * 60_000L,
        endedAtEpochMs = endedAt,
        localDate = date,
        lateCompletion = false,
    )
}
