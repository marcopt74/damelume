package com.marcopt74.damelume.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletedSessionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: CompletedSessionEntity): Long

    @Query(
        """
        SELECT COUNT(*) FROM completed_session
        WHERE mode = 'WORK'
          AND local_date = :today
          AND ended_at_epoch_ms > :sessionsResetAt
        """,
    )
    fun todayCount(
        today: String,
        sessionsResetAt: Long,
    ): Flow<Int>

    @Query("SELECT COUNT(*) FROM completed_session")
    suspend fun countAll(): Int

    @Query("SELECT * FROM completed_session ORDER BY id ASC")
    suspend fun all(): List<CompletedSessionEntity>
}
