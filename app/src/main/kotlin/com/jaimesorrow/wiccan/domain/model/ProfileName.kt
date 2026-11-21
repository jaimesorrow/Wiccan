package com.jaimesorrow.wiccan.domain.model

/** The first/last name saved on the signed-in user's `users/{uid}` profile. */
data class ProfileName(val firstName: String, val lastName: String)
