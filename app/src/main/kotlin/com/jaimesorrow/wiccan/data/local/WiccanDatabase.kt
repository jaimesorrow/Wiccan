package com.jaimesorrow.wiccan.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [CachedDailyContentEntity::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WiccanDatabase : RoomDatabase() {
    abstract fun cachedDailyContentDao(): CachedDailyContentDao

    companion object {
        const val DATABASE_NAME = "wiccan.db"
    }
}
