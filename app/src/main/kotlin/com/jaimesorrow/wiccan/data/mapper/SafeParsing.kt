package com.jaimesorrow.wiccan.data.mapper

import android.util.Log
import com.jaimesorrow.wiccan.domain.model.ContentStatus
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.ContentType
import java.util.Locale

internal const val MAPPER_LOG_TAG = "WiccanMappers"

/**
 * Parses a stored content type for display. Returns null for anything this build can't
 * render ([ContentType.UNKNOWN] from [ContentType.fromString]) so callers skip the entry.
 */
internal fun parseDisplayableTypeOrNull(raw: String?): ContentType? =
    ContentType.fromString(raw?.uppercase(Locale.ROOT)).takeIf { it != ContentType.UNKNOWN }

/**
 * Parses a stored status via [ContentStatus.fromString], upper-casing with [Locale.ROOT]
 * first so a Turkish default locale can't turn "published" into "PUBLİSHED".
 */
internal fun parseStatus(raw: String?): ContentStatus = ContentStatus.fromString(raw?.uppercase(Locale.ROOT))

/** Parses a stored tier without ever throwing; unknown, null or blank values return null. */
internal fun parseTierOrNull(raw: String?): ContentTier? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return runCatching { ContentTier.valueOf(value.uppercase(Locale.ROOT)) }.getOrNull()
}

internal fun logSkipped(message: String) {
    runCatching { Log.w(MAPPER_LOG_TAG, message) }
}
