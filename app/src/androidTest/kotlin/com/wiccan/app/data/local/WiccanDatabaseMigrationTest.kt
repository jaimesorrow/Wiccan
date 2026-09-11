package com.wiccan.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real, production MIGRATION_1_2 (see Migrations.kt) against a
 * database file actually created at schema version 1 (from the exported
 * 1.json), rather than trusting that Room.databaseBuilder wiring alone means
 * the migration is correct. Room happily lets you register a migration
 * that silently fails to reproduce the target schema; this test would catch
 * that, and any future migration added to WICCAN_DATABASE_MIGRATIONS should
 * get the same treatment.
 */
@RunWith(AndroidJUnit4::class)
class WiccanDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WiccanDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2_addsCategoryColumnDefaultingExistingRowsToMoonAndSeasonal() {
        val dbName = "migration-test"

        helper.createDatabase(dbName, 1).apply {
            execSQL(
                """
                INSERT INTO cached_daily_content
                    (contentId, type, tier, tradition, title, intro, body, materials, safetyNote, tags, status, deliveredDate)
                VALUES
                    ('content-1', 'SPELL', 'FREE', 'European folk magic', 'Candle of Clear Intent', 'intro', 'body', NULL, NULL, '[]', 'PUBLISHED', '2026-09-10')
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)

        val cursor = migrated.query("SELECT category FROM cached_daily_content WHERE contentId = 'content-1'")
        cursor.use {
            assertEquals(1, it.count)
            it.moveToFirst()
            assertEquals("MOON_AND_SEASONAL", it.getString(0))
        }
    }
}
