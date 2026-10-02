package com.jaimesorrow.wiccan.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the `category` column introduced alongside [com.jaimesorrow.wiccan.domain.model.ContentCategory].
 * Existing cached rows predate categorization, so they default to the catch-all seasonal bucket
 * until the next daily refresh overwrites them with a real, server-assigned category.
 *
 * Exposed as a top-level value (rather than private inside DatabaseModule) so the migration
 * test in androidTest exercises this exact object, not a hand-copied stand-in.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE cached_daily_content ADD COLUMN category TEXT NOT NULL DEFAULT 'MOON_AND_SEASONAL'",
        )
    }
}

/** Every migration WiccanDatabase has ever needed, in order. Never remove an entry - a device restoring an old backup may still be on any prior version. */
val WICCAN_DATABASE_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
