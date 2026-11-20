package com.jaimesorrow.wiccan.domain.model

/**
 * Kind of a content document, stored lowercase in Firestore `content.type`
 * and `userContentHistory.type` ("spell", "potion", "lesson").
 *
 * Always parse stored values with [fromString], never `valueOf`: content is
 * written by the backend and can outlive this app version, so a value this
 * build does not know (for example "ritual", which is a v2 `lessonType`, not
 * a `type`) must degrade to [UNKNOWN] instead of throwing.
 */
enum class ContentType {
    SPELL,
    POTION,
    LESSON,

    /** Fallback for a missing, blank or unrecognised stored value. Never written back. */
    UNKNOWN,
    ;

    companion object {
        /**
         * Parses a stored type. Case-insensitive and whitespace-tolerant.
         * Returns [UNKNOWN] for null, blank or unrecognised input; never throws.
         */
        fun fromString(value: String?): ContentType {
            val normalized = value?.trim()?.uppercase() ?: return UNKNOWN
            if (normalized.isEmpty()) return UNKNOWN
            return entries.firstOrNull { it != UNKNOWN && it.name == normalized } ?: UNKNOWN
        }
    }
}
