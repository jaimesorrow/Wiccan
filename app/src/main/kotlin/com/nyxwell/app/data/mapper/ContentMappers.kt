package com.nyxwell.app.data.mapper

import com.nyxwell.app.data.local.CachedDailyContentEntity
import com.nyxwell.app.domain.model.Content
import com.nyxwell.app.domain.model.ContentStatus
import com.nyxwell.app.domain.model.ContentTier
import com.nyxwell.app.domain.model.ContentType

/** Maps the raw callable-function payload into the domain [Content] model. */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toContent(): Content = Content(
    id = this["id"] as? String ?: error("Missing 'id' in content payload"),
    type = ContentType.valueOf((this["type"] as? String ?: error("Missing 'type' in content payload")).uppercase()),
    tier = ContentTier.valueOf((this["tier"] as? String ?: error("Missing 'tier' in content payload")).uppercase()),
    tradition = (this["tradition"] as? String).orEmpty(),
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
    title = title,
    intro = intro,
    body = body,
    materials = materials,
    safetyNote = safetyNote,
    tags = tags,
    status = ContentStatus.valueOf(status),
)
