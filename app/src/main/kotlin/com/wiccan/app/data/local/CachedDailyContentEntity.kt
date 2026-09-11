package com.wiccan.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room mirror of the day's selected [com.wiccan.app.domain.model.Content],
 * as returned by the `getTodayContent` Cloud Function. Single-row table:
 * [deliveredDate] lets the repository tell whether the cache is stale.
 */
@Entity(tableName = "cached_daily_content")
data class CachedDailyContentEntity(
    @PrimaryKey val contentId: String,
    val type: String,
    val tier: String,
    val tradition: String,
    val title: String,
    val intro: String,
    val body: String,
    val materials: List<String>?,
    val safetyNote: String?,
    val tags: List<String>,
    val status: String,
    val deliveredDate: String,
)
