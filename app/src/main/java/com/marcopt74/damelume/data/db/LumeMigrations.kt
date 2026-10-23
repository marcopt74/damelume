package com.marcopt74.damelume.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object LumeMigrations {
    val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_completed_session_mode_ended_at_epoch_ms` " +
                        "ON `completed_session` (`mode`, `ended_at_epoch_ms`)",
                )
            }
        }
}
