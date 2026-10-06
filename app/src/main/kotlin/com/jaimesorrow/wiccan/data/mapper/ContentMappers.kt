package com.jaimesorrow.wiccan.data.mapper

import com.jaimesorrow.wiccan.data.local.CachedDailyContentEntity
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.ContentDifficulty
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
    category = (this["category"] as? String)?.let {
        runCatching { ContentCategory.fromWireValue(it) }.getOrDefault(ContentCategory.MOON_AND_SEASONAL)
    } ?: ContentCategory.MOON_AND_SEASONAL,
    title = (this["title"] as? String).orEmpty(),
    intro = (this["intro"] as? String).orEmpty(),
    body = (this["body"] as? String).orEmpty(),
    reflection = this["reflection"] as? String,
    practice = this["practice"] as? String,
    difficulty = ContentDifficulty.fromWireValue(this["difficulty"] as? String),
    materials = this["materials"] as? List<String>,
    safetyNote = this["safetyNote"] as? String,
    tags = (this["tags"] as? List<String>).orEmpty(),
    status = ContentStatus.valueOf((this["status"] as? String ?: "published").uppercase()),
    reflection = this["reflection"] as? String,
    practice = this["practice"] as? String,
    sourceId = this["sourceId"] as? String,
    sourceCitation = this["sourceCitation"] as? String,
    sourceUrl = this["sourceUrl"] as? String,
    rightsReviewRequired = this["rightsReviewRequired"] as? Boolean,
    rightsNotes = this["rightsNotes"] as? String,
)

fun Content.toCachedEntity(deliveredDate: String, userId: String, timezone: String): CachedDailyContentEntity = CachedDailyContentEntity(
    contentId = id,
    type = type.name,
    tier = tier.name,
    tradition = tradition,
    category = category.name,
    title = title,
    intro = intro,
    body = body,
    reflection = reflection,
    practice = practice,
    difficulty = difficulty?.name,
    materials = materials,
    safetyNote = safetyNote,
    tags = tags,
    status = status.name,
    deliveredDate = deliveredDate,
    userId = userId,
    timezone = timezone,
    reflection = reflection,
    practice = practice,
    sourceId = sourceId,
    sourceCitation = sourceCitation,
    sourceUrl = sourceUrl,
    rightsReviewRequired = rightsReviewRequired,
    rightsNotes = rightsNotes,
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
    reflection = reflection,
    practice = practice,
    difficulty = ContentDifficulty.fromWireValue(difficulty),
    materials = materials,
    safetyNote = safetyNote,
    tags = tags,
    status = ContentStatus.valueOf(status),
    reflection = reflection,
    practice = practice,
    sourceId = sourceId,
    sourceCitation = sourceCitation,
    sourceUrl = sourceUrl,
    rightsReviewRequired = rightsReviewRequired,
    rightsNotes = rightsNotes,
)
