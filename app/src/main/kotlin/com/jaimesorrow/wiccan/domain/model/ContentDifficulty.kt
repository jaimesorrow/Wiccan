package com.jaimesorrow.wiccan.domain.model

/** Reader experience level, independent of the subscription tier. */
enum class ContentDifficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    ;

    companion object {
        fun fromWireValue(value: String?): ContentDifficulty? = entries.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        }
    }
}
