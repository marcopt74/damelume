package com.marcopt74.damelume.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CompletedSessionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class LumeDatabase : RoomDatabase() {
    abstract fun completedSessionDao(): CompletedSessionDao
}
