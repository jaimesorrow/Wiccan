package com.wiccan.app.domain.model

/**
 * A record that a piece of content was delivered to a user on a given date.
 * Mirrors the Firestore `userContentHistory` collection. `deliveredDate` is
 * an ISO date string (yyyy-MM-dd) set by the server.
 */
data class UserContentHistory(
    val userId: String,
    val contentId: String,
    val deliveredDate: String,
)
