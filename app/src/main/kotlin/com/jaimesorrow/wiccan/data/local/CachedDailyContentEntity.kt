package com.jaimesorrow.wiccan.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room mirror of the day's selected [com.jaimesorrow.wiccan.domain.model.Content],
 * as returned by the `getTodayContent` Cloud Function. Single-row table:
 * [deliveredDate] lets the repository tell whether the cache is stale.
 */
@Entity(tableName = "cached_daily_content")
data class CachedDailyContentEntity(
    @PrimaryKey val contentId: String,
    val type: String,
    val tier: String,
    val tradition: String,
    val category: String,
    val title: String,
    val intro: String,
    val body: String,
    val materials: List<String>?,
    val safetyNote: String?,
    val tags: List<String>,
    val status: String,
    val deliveredDate: String,
    val userId: String? = null,
    val timezone: String? = null,
    val reflection: String? = null,
    val practice: String? = null,
    val sourceId: String? = null,
    val sourceCitation: String? = null,
    val sourceUrl: String? = null,
    val rightsReviewRequired: Boolean? = null,
    val rightsNotes: String? = null,
    val summary: String? = null,
    val primaryCategoryId: String? = null,
    val secondaryCategoryIds: List<String>? = null,
    val lessonType: String? = null,
    val requiresPotionDisclosure: Boolean = false,
    val potionSafetyDisclosure: String? = null,
    val durationMinutes: Int? = null,
    val tools: List<String>? = null,
    val incantation: String? = null,
    val incantationOmissionReason: String? = null,
    val timing: String? = null,
    val application: String? = null,
    val correspondences: List<String>? = null,
    val adaptationNotes: List<String>? = null,
    val sourcePassageId: String? = null,
    val citationTextTitle: String? = null,
    val citationManuscriptId: String? = null,
    val citationTradition: String? = null,
    val citationManuscriptDate: String? = null,
    val citationCompositionDate: String? = null,
    val citationEdition: String? = null,
    val citationEditionDate: String? = null,
    val citationTranslator: String? = null,
    val citationLocator: String? = null,
    val citationUrl: String? = null,
)
