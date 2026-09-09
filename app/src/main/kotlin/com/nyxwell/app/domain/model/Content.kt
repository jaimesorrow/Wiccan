package com.nyxwell.app.domain.model

/**
 * A single piece of daily content (spell, potion, or lesson).
 * Mirrors the Firestore `content` collection.
 */
data class Content(
    val id: String,
    val type: ContentType,
    val tier: ContentTier,
    val tradition: String,
    val title: String,
    val intro: String,
    val body: String,
    val materials: List<String>?,
    val safetyNote: String?,
    val tags: List<String>,
    val status: ContentStatus,
)
