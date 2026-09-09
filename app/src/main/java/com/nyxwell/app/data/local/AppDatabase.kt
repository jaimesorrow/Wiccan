package com.nyxwell.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [CachedDailyContent::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedDailyContentDao(): CachedDailyContentDao
}
