package com.jaimesorrow.wiccan.data.local

import android.content.Context
import androidx.room.Room
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Opens exported legacy schemas through Room so its real migration validator runs. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class WiccanDatabaseMigrationJvmTest {
    @Test
    fun `version 2 cached content survives upgrade to structured lessons`() = migrateLegacyDatabase(2)

    @Test
    fun `version 1 installations can upgrade directly to the current schema`() = migrateLegacyDatabase(1)

    private fun migrateLegacyDatabase(version: Int) {
        val context = RuntimeEnvironment.getApplication()
        val name = "lesson-migration-v$version.db"
        context.deleteDatabase(name)
        val schema = JSONObject(
            File("schemas/com.jaimesorrow.wiccan.data.local.WiccanDatabase/$version.json").readText(),
        ).getJSONObject("database")
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { legacy ->
            val tables = schema.getJSONArray("entities")
            for (index in 0 until tables.length()) {
                val table = tables.getJSONObject(index)
                legacy.execSQL(table.getString("createSql").replace("\${TABLE_NAME}", table.getString("tableName")))
            }
            val setupQueries = schema.getJSONArray("setupQueries")
            for (index in 0 until setupQueries.length()) legacy.execSQL(setupQueries.getString(index))
            val categoryColumn = if (version >= 2) ", category" else ""
            val categoryValue = if (version >= 2) ", 'HOME_AND_HEARTH'" else ""
            legacy.execSQL(
                "INSERT INTO cached_daily_content " +
                    "(contentId, type, tier, tradition, title, intro, body, tags, status, deliveredDate$categoryColumn) " +
                    "VALUES ('lesson-1', 'LESSON', 'FREE', 'Folk practice', 'The Threshold', 'intro', 'body', '', " +
                    "'PUBLISHED', '2026-10-02'$categoryValue)",
            )
            legacy.version = version
        }
        val database = Room.databaseBuilder(context, WiccanDatabase::class.java, name)
            .addMigrations(*WICCAN_DATABASE_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            // Opening runs Room's target-schema validation, not only the ALTER statements.
            val migrated = database.openHelper.writableDatabase
            migrated.query(
                "SELECT body, category, reflection, practice, primaryCategoryId, sourcePassageId, " +
                    "requiresPotionDisclosure FROM cached_daily_content",
            ).use {
                assertTrue(it.moveToFirst())
                assertEquals("body", it.getString(0))
                assertEquals(if (version == 1) "MOON_AND_SEASONAL" else "HOME_AND_HEARTH", it.getString(1))
                assertTrue(it.isNull(2))
                assertTrue(it.isNull(3))
                assertTrue(it.isNull(4))
                assertTrue(it.isNull(5))
                assertEquals(0, it.getInt(6))
            }
            migrated.execSQL("UPDATE cached_daily_content SET reflection = 'Which boundary matters?'")
            migrated.query("SELECT reflection FROM cached_daily_content").use {
                assertTrue(it.moveToFirst())
                assertEquals("Which boundary matters?", it.getString(0))
            }
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
