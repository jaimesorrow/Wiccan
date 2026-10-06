package com.jaimesorrow.wiccan.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Builds historical databases from the checked-in exported schemas and opens
 * them through the production Room class. Room validates the upgraded schema,
 * while assertions prove that old lessons survive. Reading schema JSON with
 * Android's parser avoids the migration helper's serializer ABI incompatibility
 * with this project's existing runtime dependencies.
 */
@RunWith(AndroidJUnit4::class)
class WiccanDatabaseMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val dbName = "wiccan-room-migration-test.db"
    private var roomDatabase: WiccanDatabase? = null

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        roomDatabase?.close()
        context.deleteDatabase(dbName)
    }

    private fun createDatabase(version: Int): SQLiteDatabase {
        val schema = instrumentation.context.assets.open("${WiccanDatabase::class.java.name}/$version.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val path = context.getDatabasePath(dbName)
        path.parentFile?.mkdirs()
        return SQLiteDatabase.openOrCreateDatabase(path, null).apply {
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val queries = schema.getJSONArray("setupQueries")
            for (index in 0 until queries.length()) execSQL(queries.getString(index))
            this.version = version
        }
    }

    private fun openCurrentDatabase(): SupportSQLiteDatabase {
        val database = Room.databaseBuilder(context, WiccanDatabase::class.java, dbName)
            .addMigrations(*WICCAN_DATABASE_MIGRATIONS)
            .build()
        roomDatabase = database
        return database.openHelper.writableDatabase
    }

    @Test
    fun migrate1To2_addsCategoryColumnDefaultingExistingRowsToMoonAndSeasonal() {
        createDatabase(1).apply {
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

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(dbName).callback(
                object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) = error("Historical database must already exist")

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        assertEquals(1, oldVersion)
                        assertEquals(2, newVersion)
                        MIGRATION_1_2.migrate(db)
                    }
                },
            ).build(),
        )
        try {
            helper.writableDatabase.query("SELECT category FROM cached_daily_content WHERE contentId = 'content-1'").use {
                assertEquals(1, it.count)
                it.moveToFirst()
                assertEquals("MOON_AND_SEASONAL", it.getString(0))
            }
        } finally {
            helper.close()
        }
    }

    @Test
    fun migrate1To3_preservesHistoricalLessonWhileAddingNullableMetadata() {
        createDatabase(1).apply {
            execSQL(
                """
                INSERT INTO cached_daily_content
                    (contentId, type, tier, tradition, title, intro, body, materials, safetyNote, tags, status, deliveredDate)
                VALUES
                    ('old-lesson', 'LESSON', 'FREE', 'Contemporary Wicca', 'Old title', 'Old introduction', 'Old lesson', NULL, 'Old safety note', '[]', 'PUBLISHED', '2025-01-01')
                """.trimIndent(),
            )
            close()
        }
        val migrated = openCurrentDatabase()
        migrated.query(
            "SELECT title, intro, body, safetyNote, deliveredDate, category, reflection, rightsNotes, userId " +
                "FROM cached_daily_content WHERE contentId = 'old-lesson'",
        ).use {
            assertTrue(it.moveToFirst())
            assertEquals("Old title", it.getString(0))
            assertEquals("Old introduction", it.getString(1))
            assertEquals("Old lesson", it.getString(2))
            assertEquals("Old safety note", it.getString(3))
            assertEquals("2025-01-01", it.getString(4))
            assertEquals("MOON_AND_SEASONAL", it.getString(5))
            assertTrue(it.isNull(6))
            assertTrue(it.isNull(7))
            assertTrue(it.isNull(8))
        }
    }

    @Test
    fun migrate2To3_preservesExistingCategoryAndLessonFields() {
        createDatabase(2).apply {
            execSQL(
                """
                INSERT INTO cached_daily_content
                    (contentId, type, tier, tradition, category, title, intro, body, materials, safetyNote, tags, status, deliveredDate)
                VALUES
                    ('old-lesson', 'LESSON', 'PREMIUM', 'Contemporary Wicca', 'PROTECTION', 'Old title', 'Old intro', 'Old lesson', NULL, 'Safe practice only', '[]', 'PUBLISHED', '2025-01-01')
                """.trimIndent(),
            )
            close()
        }
        val migrated = openCurrentDatabase()
        migrated.query(
            "SELECT category, body, safetyNote, timezone, rightsReviewRequired FROM cached_daily_content WHERE contentId = 'old-lesson'",
        ).use {
            assertTrue(it.moveToFirst())
            assertEquals("PROTECTION", it.getString(0))
            assertEquals("Old lesson", it.getString(1))
            assertEquals("Safe practice only", it.getString(2))
            assertTrue(it.isNull(3))
            assertTrue(it.isNull(4))
        }
    }
}
