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
)
