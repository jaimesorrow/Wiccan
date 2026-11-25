package com.jaimesorrow.wiccan.domain.model

/** Concealed community handle. The server reserves the lowercase form. */
object UsernameRules {
    const val MIN_LENGTH = 3
    const val MAX_LENGTH = 24
    val PATTERN = Regex("^[A-Za-z0-9_]{$MIN_LENGTH,$MAX_LENGTH}$")

    fun isValid(value: String): Boolean = PATTERN.matches(value.trim())
}
