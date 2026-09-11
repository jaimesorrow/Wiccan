package com.wiccan.app.domain.model

/** One row of a user's delivered-content history, for the Archive list. */
data class HistoryEntry(
    val contentId: String,
    val title: String,
    val type: ContentType,
    val tradition: String,
    val category: ContentCategory,
    /** ISO date string (yyyy-MM-dd). */
    val deliveredDate: String,
)
