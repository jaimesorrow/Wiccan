package com.nyxwell.app.domain.model

data class UserContentHistory(
    val userId: String,
    val contentId: String,
    val deliveredDate: String,
)
