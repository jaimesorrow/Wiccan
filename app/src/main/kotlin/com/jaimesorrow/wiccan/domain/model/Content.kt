package com.jaimesorrow.wiccan.domain.model

/**
 * A single piece of daily content (spell, potion, or lesson).
 * Mirrors the Firestore `content` collection.
 */
data class Content(
    val id: String,
    val type: ContentType,
    val tier: ContentTier,
    val tradition: String,
    val category: ContentCategory,
    val title: String,
    val intro: String,
    val body: String,
    val materials: List<String>?,
    val safetyNote: String?,
    val tags: List<String>,
    val status: ContentStatus,
    val reflection: String? = null,
    val practice: String? = null,
    val sourceId: String? = null,
    val sourceCitation: String? = null,
    val sourceUrl: String? = null,
    val rightsReviewRequired: Boolean? = null,
    val rightsNotes: String? = null,
    val summary: String? = null,
    val primaryCategoryId: String? = null,
    val secondaryCategoryIds: List<String> = emptyList(),
    val lessonType: String? = null,
    val requiresPotionDisclosure: Boolean = false,
    val potionSafetyDisclosure: String? = null,
    val durationMinutes: Int? = null,
    val tools: List<String> = emptyList(),
    val incantation: String? = null,
    val incantationOmissionReason: String? = null,
    val timing: String? = null,
    val application: String? = null,
    val correspondences: List<String> = emptyList(),
    val adaptationNotes: List<String> = emptyList(),
    val sourcePassageId: String? = null,
    val sourceTradition: String? = null,
    val citation: ContentCitation? = null,
)

data class ContentCitation(
    val textTitle: String? = null,
    val manuscriptId: String? = null,
    val tradition: String? = null,
    val manuscriptDate: String? = null,
    val compositionDate: String? = null,
    val edition: String? = null,
    val editionDate: String? = null,
    val translator: String? = null,
    val locator: String? = null,
    val url: String? = null,
)
