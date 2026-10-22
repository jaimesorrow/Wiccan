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

/** Adds optional lesson sections and assignment provenance without rewriting existing rows. */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf(
            "userId",
            "timezone",
            "reflection",
            "practice",
            "sourceId",
            "sourceCitation",
            "sourceUrl",
            "rightsNotes",
        ).forEach { column ->
            db.execSQL("ALTER TABLE cached_daily_content ADD COLUMN $column TEXT")
        }
        db.execSQL("ALTER TABLE cached_daily_content ADD COLUMN rightsReviewRequired INTEGER")
    }
}

/** Adds the practical lesson, disclosure, citation, and adaptation fields without replacing cached history. */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf(
            "summary",
            "primaryCategoryId",
            "secondaryCategoryIds",
            "lessonType",
            "potionSafetyDisclosure",
            "tools",
            "incantation",
            "incantationOmissionReason",
            "timing",
            "application",
            "correspondences",
            "adaptationNotes",
            "sourcePassageId",
            "citationTextTitle",
            "citationManuscriptId",
            "citationTradition",
            "citationManuscriptDate",
            "citationCompositionDate",
            "citationEdition",
            "citationEditionDate",
            "citationTranslator",
            "citationLocator",
            "citationUrl",
        ).forEach { column ->
            db.execSQL("ALTER TABLE cached_daily_content ADD COLUMN $column TEXT")
        }
        db.execSQL(
            "ALTER TABLE cached_daily_content ADD COLUMN requiresPotionDisclosure INTEGER NOT NULL DEFAULT 0",
        )
        db.execSQL("ALTER TABLE cached_daily_content ADD COLUMN durationMinutes INTEGER")
    }
}

/** Every migration WiccanDatabase has ever needed, in order. Never remove an entry - a device restoring an old backup may still be on any prior version. */
val WICCAN_DATABASE_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)

