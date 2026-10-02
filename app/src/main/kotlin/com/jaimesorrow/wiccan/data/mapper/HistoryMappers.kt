package com.jaimesorrow.wiccan.data.mapper

import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.ContentType
import com.jaimesorrow.wiccan.domain.model.HistoryEntry

fun Map<String, Any?>.toHistoryEntry(): HistoryEntry = HistoryEntry(
    contentId = this["contentId"] as? String ?: error("Missing 'contentId' in history payload"),
    title = (this["title"] as? String).orEmpty(),
    type = ContentType.valueOf((this["type"] as? String ?: error("Missing 'type' in history payload")).uppercase()),
    tradition = (this["tradition"] as? String).orEmpty(),
    category = ContentCategory.fromWireValue(
        this["category"] as? String ?: error("Missing 'category' in history payload"),
    ),
    deliveredDate = this["deliveredDate"] as? String ?: error("Missing 'deliveredDate' in history payload"),
)
