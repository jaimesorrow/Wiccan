package com.jaimesorrow.wiccan.data.mapper

import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.ContentType
import com.jaimesorrow.wiccan.domain.model.HistoryEntry
import java.util.Locale

fun Map<String, Any?>.toHistoryEntry(): HistoryEntry = HistoryEntry(
    contentId = this["contentId"] as? String ?: error("Missing 'contentId' in history payload"),
    title = (this["title"] as? String).orEmpty(),
    type = ContentType.valueOf((this["type"] as? String ?: error("Missing 'type' in history payload")).uppercase(Locale.ROOT)),
    tradition = (this["tradition"] as? String).orEmpty(),
    category = (this["category"] as? String)?.let {
        runCatching { ContentCategory.fromWireValue(it) }.getOrDefault(ContentCategory.MOON_AND_SEASONAL)
    } ?: ContentCategory.MOON_AND_SEASONAL,
    deliveredDate = this["deliveredDate"] as? String ?: error("Missing 'deliveredDate' in history payload"),
)

/** Legacy or malformed history rows must not cancel the Archive stream. */
fun Map<String, Any?>.toHistoryEntryOrNull(): HistoryEntry? = runCatching { toHistoryEntry() }.getOrNull()
