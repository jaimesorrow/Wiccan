package com.nyxwell.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [CachedDailyContentEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class NyxwellDatabase : RoomDatabase() {
    abstract fun cachedDailyContentDao(): CachedDailyContentDao

    companion object {
        const val DATABASE_NAME = "nyxwell.db"
    }
}
