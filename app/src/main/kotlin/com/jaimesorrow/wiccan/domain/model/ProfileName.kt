package com.jaimesorrow.wiccan.domain.model

/** The personal and community-safe profile fields saved on `users/{uid}`. */
data class ProfileName(val firstName: String, val lastName: String, val username: String = "")
