package com.jaimesorrow.wiccan.data.mapper

import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.HistoryEntry

/**
 * Maps one `userContentHistory` row. Never throws: a row with a missing id or date, or
 * a content type this build can't display (`ContentType.UNKNOWN`), is logged and
 * returns null so the Grimoire skips it instead of crashing.
 */
fun Map<String, Any?>.toHistoryEntryOrNull(): HistoryEntry? {
    val contentId = this["contentId"] as? String
    val deliveredDate = this["deliveredDate"] as? String
    val rawType = this["type"] as? String
    val type = parseDisplayableTypeOrNull(rawType)
    if (contentId.isNullOrBlank() || deliveredDate.isNullOrBlank() || type == null) {
        logSkipped("Skipping history row '$contentId' ($deliveredDate): type='$rawType'")
        return null
    }
    return HistoryEntry(
        contentId = contentId,
        title = (this["title"] as? String).orEmpty(),
        type = type,
        tradition = (this["tradition"] as? String).orEmpty(),
        category = (this["category"] as? String)?.let {
            runCatching { ContentCategory.fromWireValue(it) }.getOrDefault(ContentCategory.MOON_AND_SEASONAL)
        } ?: ContentCategory.MOON_AND_SEASONAL,
        deliveredDate = deliveredDate,
    )
}

/** Maps a batch of history rows, dropping any row [toHistoryEntryOrNull] can't read. */
fun List<Map<String, Any?>>.toHistoryEntries(): List<HistoryEntry> = mapNotNull { it.toHistoryEntryOrNull() }
