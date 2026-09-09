package com.nyxwell.app.domain.model

data class Content(
    val id: String,
    val type: ContentType,
    val tier: ContentTier,
    val tradition: String,
    val title: String,
    val intro: String,
    val body: String,
    val materials: List<String> = emptyList(),
    val safetyNote: String? = null,
    val tags: List<String> = emptyList(),
    val status: ContentStatus,
)
