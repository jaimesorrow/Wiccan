package com.jaimesorrow.wiccan.data.mapper

import com.jaimesorrow.wiccan.data.local.CachedDailyContentEntity
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.ContentCitation
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.ContentType

/**
 * Maps the raw callable-function or Firestore payload into the domain [Content] model.
 * Never throws: a payload with a missing id or an unknown/missing type or tier is
 * logged and returns null so one bad document can't crash or blank a screen.
 */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toContentOrNull(): Content? = runCatching { toContentInternal() }
    .onFailure { logSkipped("Skipping unreadable content '${this["id"]}': ${it.message}") }
    .getOrNull()

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.toContentInternal(): Content? {
    val id = this["id"] as? String
    val rawType = this["type"] as? String
    val rawTier = this["tier"] as? String
    val type = parseDisplayableTypeOrNull(rawType)
    val tier = parseTierOrNull(rawTier)
    if (id.isNullOrBlank() || type == null || tier == null) {
        logSkipped("Skipping content '$id': type='$rawType' tier='$rawTier'")
        return null
    }
    val structuredCitation = (this["citations"] as? List<*>)?.firstOrNull() as? Map<String, Any?>
    val toolNames = (this["tools"] ?: this["materials"]).toToolNames()
    return Content(
        id = id,
        type = type,
        tier = tier,
        tradition = (this["tradition"] as? String).orEmpty(),
        category = ((this["primaryCategoryId"] ?: this["category"]) as? String)?.let {
            runCatching { ContentCategory.fromWireValue(it) }.getOrDefault(ContentCategory.MOON_AND_SEASONAL)
        } ?: ContentCategory.MOON_AND_SEASONAL,
        title = (this["title"] as? String).orEmpty(),
        intro = ((this["introduction"] ?: this["intro"]) as? String).orEmpty(),
        body = ((this["lesson"] ?: this["body"]) as? String).orEmpty(),
        reflection = this["reflection"] as? String,
        practice = this["practice"] as? String,
        materials = toolNames.ifEmpty { null },
        safetyNote = this["safetyNote"] as? String,
        tags = (this["tags"] as? List<*>).orEmpty().filterIsInstance<String>(),
        status = parseStatus((this["contentStatus"] ?: this["status"]) as? String ?: "published"),
        sourceId = this["sourceId"] as? String,
        sourceCitation = this["sourceCitation"] as? String,
        sourceUrl = this["sourceUrl"] as? String,
        rightsReviewRequired = this["rightsReviewRequired"] as? Boolean,
        rightsNotes = this["rightsNotes"] as? String,
        summary = this["summary"] as? String,
        primaryCategoryId = this["primaryCategoryId"] as? String,
        secondaryCategoryIds = (this["secondaryCategoryIds"] as? List<String>).orEmpty(),
        lessonType = this["lessonType"] as? String,
        requiresPotionDisclosure = this["requiresPotionDisclosure"] as? Boolean ?: false,
        potionSafetyDisclosure = this["potionSafetyDisclosure"] as? String,
        durationMinutes = (this["durationMinutes"] as? Number)?.toInt(),
        tools = toolNames,
        incantation = this["incantation"] as? String,
        incantationOmissionReason = this["incantationOmissionReason"] as? String,
        timing = this["timing"] as? String,
        application = this["application"] as? String,
        correspondences = (this["correspondences"] as? List<String>).orEmpty(),
        adaptationNotes = (this["adaptationNotes"] as? List<String>).orEmpty(),
        sourcePassageId = this["sourcePassageId"] as? String,
        sourceTradition = (this["sourceTradition"] as? String) ?: (structuredCitation?.get("tradition") as? String),
        illustrationKey = (this["illustrationKey"] as? String)?.trim()?.takeIf { it.isNotEmpty() },
        illustrationUrl = (this["illustrationUrl"] as? String)?.trim()?.takeIf { it.startsWith("https://") },
        citation = structuredCitation?.toCitation(),
    )
}

private fun Any?.toToolNames(): List<String> = (this as? List<*>)
    .orEmpty()
    .mapNotNull { value ->
        when (value) {
            is String -> value
            is Map<*, *> -> value["name"] as? String
            else -> null
        }?.trim()?.takeIf { it.isNotEmpty() }
    }

private fun Map<String, Any?>.toCitation(): ContentCitation = ContentCitation(
    textTitle = this["textTitle"] as? String,
    manuscriptId = this["manuscriptId"] as? String,
    tradition = this["tradition"] as? String,
    manuscriptDate = this["manuscriptDate"] as? String,
    compositionDate = this["compositionDate"] as? String,
    edition = this["edition"] as? String,
    editionDate = this["editionDate"] as? String,
    translator = this["translator"] as? String,
    locator = this["locator"] as? String,
    url = this["url"] as? String,
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
    materials = materials,
    safetyNote = safetyNote,
    tags = tags,
    status = status.name,
    deliveredDate = deliveredDate,
    userId = userId,
    timezone = timezone,
    sourceId = sourceId,
    sourceCitation = sourceCitation,
    sourceUrl = sourceUrl,
    rightsReviewRequired = rightsReviewRequired,
    rightsNotes = rightsNotes,
    summary = summary,
    primaryCategoryId = primaryCategoryId,
    secondaryCategoryIds = secondaryCategoryIds,
    lessonType = lessonType,
    requiresPotionDisclosure = requiresPotionDisclosure,
    potionSafetyDisclosure = potionSafetyDisclosure,
    durationMinutes = durationMinutes,
    tools = tools,
    incantation = incantation,
    incantationOmissionReason = incantationOmissionReason,
    timing = timing,
    application = application,
    correspondences = correspondences,
    adaptationNotes = adaptationNotes,
    sourcePassageId = sourcePassageId,
    sourceTradition = sourceTradition,
    illustrationKey = illustrationKey,
    illustrationUrl = illustrationUrl,
    citationTextTitle = citation?.textTitle,
    citationManuscriptId = citation?.manuscriptId,
    citationTradition = citation?.tradition,
    citationManuscriptDate = citation?.manuscriptDate,
    citationCompositionDate = citation?.compositionDate,
    citationEdition = citation?.edition,
    citationEditionDate = citation?.editionDate,
    citationTranslator = citation?.translator,
    citationLocator = citation?.locator,
    citationUrl = citation?.url,
)

/** Never throws: a cached row this build can't read (unknown type or tier) maps to null. */
fun CachedDailyContentEntity.toDomainOrNull(): Content? {
    val parsedType = parseDisplayableTypeOrNull(type)
    val parsedTier = parseTierOrNull(tier)
    if (parsedType == null || parsedTier == null) {
        logSkipped("Skipping cached content '$contentId': type='$type' tier='$tier'")
        return null
    }
    return runCatching { toDomain(parsedType, parsedTier) }
        .onFailure { logSkipped("Skipping cached content '$contentId': ${it.message}") }
        .getOrNull()
}

private fun CachedDailyContentEntity.toDomain(parsedType: ContentType, parsedTier: ContentTier): Content = Content(
    id = contentId,
    type = parsedType,
    tier = parsedTier,
    tradition = tradition,
    category = ContentCategory.fromStoredValue(category),
    title = title,
    intro = intro,
    body = body,
    reflection = reflection,
    practice = practice,
    materials = materials,
    safetyNote = safetyNote,
    tags = tags,
    status = parseStatus(status),
    sourceId = sourceId,
    sourceCitation = sourceCitation,
    sourceUrl = sourceUrl,
    rightsReviewRequired = rightsReviewRequired,
    rightsNotes = rightsNotes,
    summary = summary,
    primaryCategoryId = primaryCategoryId,
    secondaryCategoryIds = secondaryCategoryIds.orEmpty(),
    lessonType = lessonType,
    requiresPotionDisclosure = requiresPotionDisclosure,
    potionSafetyDisclosure = potionSafetyDisclosure,
    durationMinutes = durationMinutes,
    tools = tools.orEmpty(),
    incantation = incantation,
    incantationOmissionReason = incantationOmissionReason,
    timing = timing,
    application = application,
    correspondences = correspondences.orEmpty(),
    adaptationNotes = adaptationNotes.orEmpty(),
    sourcePassageId = sourcePassageId,
    sourceTradition = sourceTradition,
    illustrationKey = illustrationKey,
    illustrationUrl = illustrationUrl,
    citation = if (citationTextTitle == null && citationLocator == null) {
        null
    } else {
        ContentCitation(
            textTitle = citationTextTitle,
            manuscriptId = citationManuscriptId,
            tradition = citationTradition,
            manuscriptDate = citationManuscriptDate,
            compositionDate = citationCompositionDate,
            edition = citationEdition,
            editionDate = citationEditionDate,
            translator = citationTranslator,
            locator = citationLocator,
            url = citationUrl,
        )
    },
)
