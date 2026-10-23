package com.marcopt74.damelume.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "completed_session",
    indices = [Index(value = ["local_date"])],
)
data class CompletedSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    @ColumnInfo(name = "planned_ms") val plannedMs: Long,
    @ColumnInfo(name = "ended_at_epoch_ms") val endedAtEpochMs: Long,
    @ColumnInfo(name = "local_date") val localDate: String,
    @ColumnInfo(name = "late_completion") val lateCompletion: Boolean,
)
