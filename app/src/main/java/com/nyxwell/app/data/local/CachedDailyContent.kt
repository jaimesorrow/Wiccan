package com.nyxwell.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nyxwell.app.domain.model.ContentStatus
import com.nyxwell.app.domain.model.ContentTier
import com.nyxwell.app.domain.model.ContentType

const val DAILY_CONTENT_CACHE_KEY = "today"

@Entity(tableName = "cached_daily_content")
data class CachedDailyContent(
    @PrimaryKey val cacheKey: String = DAILY_CONTENT_CACHE_KEY,
    val id: String,
    val type: ContentType,
    val tier: ContentTier,
    val tradition: String,
    val title: String,
    val intro: String,
    val body: String,
    val materials: List<String>,
    val safetyNote: String?,
    val tags: List<String>,
    val status: ContentStatus,
    val cachedDate: String,
)
