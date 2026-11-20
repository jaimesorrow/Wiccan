package com.jaimesorrow.wiccan.domain.model

/**
 * Editorial status of a content document (`content.status`, or the v2
 * `content.contentStatus`, which can also be "review", "rejected" or
 * "archived").
 *
 * Always parse stored values with [fromString], never `valueOf`.
 */
enum class ContentStatus {
    DRAFT,
    PUBLISHED,
    ;

    companion object {
        /**
         * Parses a stored status. Case-insensitive and whitespace-tolerant; never throws.
         *
         * Only an explicit "published" maps to [PUBLISHED]. Null, blank and every other
         * value ("review", "rejected", "archived", typos, future values) map to [DRAFT],
         * so an unrecognised status is never treated as published. Callers that want
         * a missing field to mean published (as the legacy content mapper does) should
         * substitute "published" before calling.
         */
        fun fromString(value: String?): ContentStatus =
            if (value?.trim()?.uppercase() == PUBLISHED.name) PUBLISHED else DRAFT
    }
}
