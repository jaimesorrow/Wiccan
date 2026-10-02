package com.jaimesorrow.wiccan.data.mapper

import com.jaimesorrow.wiccan.data.local.CachedDailyContentEntity
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.ContentStatus
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.ContentType

/** Maps the raw callable-function payload into the domain [Content] model. */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toContent(): Content = Content(
    id = this["id"] as? String ?: error("Missing 'id' in content payload"),
    type = ContentType.valueOf((this["type"] as? String ?: error("Missing 'type' in content payload")).uppercase()),
    tier = ContentTier.valueOf((this["tier"] as? String ?: error("Missing 'tier' in content payload")).uppercase()),
    tradition = (this["tradition"] as? String).orEmpty(),
    category = ContentCategory.fromWireValue(
        this["category"] as? String ?: error("Missing 'category' in content payload"),
    ),
    title = (this["title"] as? String).orEmpty(),
    intro = (this["intro"] as? String).orEmpty(),
    body = (this["body"] as? String).orEmpty(),
    materials = this["materials"] as? List<String>,
    safetyNote = this["safetyNote"] as? String,
    tags = (this["tags"] as? List<String>).orEmpty(),
    status = ContentStatus.valueOf((this["status"] as? String ?: "published").uppercase()),
)

fun Content.toCachedEntity(deliveredDate: String): CachedDailyContentEntity = CachedDailyContentEntity(
    contentId = id,
    type = type.name,
    tier = tier.name,
    tradition = tradition,
    category = category.name,
    title = title,
    intro = intro,
    body = body,
    materials = materials,
    safetyNote = safetyNote,
    tags = tags,
    status = status.name,
    deliveredDate = deliveredDate,
)

fun CachedDailyContentEntity.toDomain(): Content = Content(
    id = contentId,
    type = ContentType.valueOf(type),
    tier = ContentTier.valueOf(tier),
    tradition = tradition,
    category = ContentCategory.valueOf(category),
    title = title,
    intro = intro,
    body = body,
    materials = materials,
    safetyNote = safetyNote,
    tags = tags,
    status = ContentStatus.valueOf(status),
)
