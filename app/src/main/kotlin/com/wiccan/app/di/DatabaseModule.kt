package com.wiccan.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wiccan.app.data.local.CachedDailyContentDao
import com.wiccan.app.data.local.WiccanDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Adds the `category` column introduced alongside [com.wiccan.app.domain.model.ContentCategory].
 * Existing cached rows predate categorization, so they default to the catch-all seasonal bucket
 * until the next daily refresh overwrites them with a real, server-assigned category.
 */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE cached_daily_content ADD COLUMN category TEXT NOT NULL DEFAULT 'MOON_AND_SEASONAL'",
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWiccanDatabase(@ApplicationContext context: Context): WiccanDatabase =
        Room.databaseBuilder(context, WiccanDatabase::class.java, WiccanDatabase.DATABASE_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideCachedDailyContentDao(database: WiccanDatabase): CachedDailyContentDao =
        database.cachedDailyContentDao()
}
