package com.nyxwell.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedDailyContentDao {
    @Query("SELECT * FROM cached_daily_content LIMIT 1")
    fun observeTodayContent(): Flow<CachedDailyContent?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(content: CachedDailyContent)

    @Query("DELETE FROM cached_daily_content")
    suspend fun clear()
}
