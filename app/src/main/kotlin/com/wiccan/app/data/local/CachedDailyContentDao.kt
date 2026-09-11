package com.wiccan.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedDailyContentDao {

    @Query("SELECT * FROM cached_daily_content LIMIT 1")
    fun observe(): Flow<CachedDailyContentEntity?>

    @Query("SELECT * FROM cached_daily_content LIMIT 1")
    suspend fun get(): CachedDailyContentEntity?

    @Upsert
    suspend fun upsert(entity: CachedDailyContentEntity)

    @Query("DELETE FROM cached_daily_content")
    suspend fun clear()

    /** Replaces the single cached row atomically. */
    @Transaction
    suspend fun replace(entity: CachedDailyContentEntity) {
        clear()
        upsert(entity)
    }
}
