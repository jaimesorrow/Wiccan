package com.jaimesorrow.wiccan.data.mapper

import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.ContentStatus
import com.jaimesorrow.wiccan.domain.model.ContentType
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ContentMappersTest {
    private val lesson = mapOf<String, Any?>(
        "id" to "legacy-lesson",
        "type" to "lesson",
        "tier" to "free",
        "title" to "Seasonal observation",
        "tradition" to "Contemporary Wicca",
        "intro" to "Notice the season.",
        "body" to "Observe your surroundings.",
        "status" to "published",
    )

    @Test
    fun `historical content without category or new optional fields remains readable`() {
        val content = lesson.toContentOrNull()!!
        assertEquals(ContentCategory.MOON_AND_SEASONAL, content.category)
        assertEquals("Notice the season.", content.intro)
        assertEquals("Observe your surroundings.", content.body)
        assertNull(content.reflection)
        assertNull(content.rightsNotes)
    }

    @Test
    fun `all approved lesson sections and rights metadata survive Room round trip`() {
        val content = (
            lesson + mapOf(
                "category" to "Protection",
                "reflection" to "What offers reassurance?",
                "practice" to "Sit quietly and observe your breathing.",
                "safetyNote" to "Avoid smoke and flames.",
                "sourceId" to "reviewed-source",
                "sourceCitation" to "Author, Modern Practice (2024)",
                "sourceUrl" to "https://example.com/source",
                "rightsReviewRequired" to false,
                "rightsNotes" to "Original synthesis with attribution",
                "sourceTradition" to "Roman-period Egyptian magical practice",
            )
            ).toContentOrNull()!!
        assertEquals(content, content.toCachedEntity("2026-10-06", "user-1", "Europe/London").toDomainOrNull())
        assertEquals(ContentCategory.PROTECTION, content.category)
        assertEquals("Roman-period Egyptian magical practice", content.sourceTradition)
    }

    @Test
    fun `legacy difficulty data does not become a field or affect content mapping`() {
        assertEquals(lesson.toContentOrNull()!!, (lesson + ("difficulty" to "advanced")).toContentOrNull()!!)
        assertFalse(lesson.toContentOrNull()!!.javaClass.declaredFields.any { it.name == "difficulty" })
    }

    @Test
    fun `all proposed practical category ids remain readable`() {
        val expected = mapOf(
            "candle-spells" to ContentCategory.CANDLE_SPELLS,
            "herbal-rituals" to ContentCategory.HERBAL_RITUALS,
            "cord-knot-work" to ContentCategory.CORD_KNOT_WORK,
            "lunar-practices" to ContentCategory.LUNAR_PRACTICES,
            "charms-talismans" to ContentCategory.CHARMS_TALISMANS,
            "candle-magic" to ContentCategory.CANDLE_MAGIC,
            "herbs-botanicals" to ContentCategory.HERBS_BOTANICALS,
            "potions-infusions" to ContentCategory.POTIONS_INFUSIONS,
            "crystals-stones" to ContentCategory.CRYSTALS_STONES,
            "cord-knot-magic" to ContentCategory.CORD_KNOT_MAGIC,
            "moon-phases-timing" to ContentCategory.MOON_MAGIC_TIMING,
            "elemental-magic" to ContentCategory.ELEMENTAL_MAGIC,
            "altars-sacred-space" to ContentCategory.ALTARS_SACRED_SPACE,
            "divination" to ContentCategory.DIVINATION,
            "elemental-practices" to ContentCategory.ELEMENTAL_PRACTICES,
            "protection-cleansing-warding" to ContentCategory.PROTECTION_CLEANSING_WARDING,
        )

        expected.forEach { (wireValue, category) ->
            assertEquals(category, (lesson + ("primaryCategoryId" to wireValue)).toContentOrNull()!!.category)
        }
    }

    @Test
    fun `canonical tool objects are projected to readable tool names`() {
        val content = (
            lesson + mapOf(
                "primaryCategoryId" to "cord-knot-work",
                "tools" to listOf(
                    mapOf("name" to "Natural-fiber cord", "quantity" to "1 length", "required" to true),
                    mapOf("name" to "Journal", "required" to false),
                ),
            )
            ).toContentOrNull()!!

        assertEquals(listOf("Natural-fiber cord", "Journal"), content.tools)
        assertEquals(listOf("Natural-fiber cord", "Journal"), content.materials)
    }

    @Test
    fun `existing history remains readable through Grimoire rename without replacing its keys`() {
        val row = mapOf<String, Any?>(
            "contentId" to "legacy-lesson",
            "title" to "Seasonal observation",
            "type" to "lesson",
            "tradition" to "Contemporary Wicca",
            "deliveredDate" to "2025-04-01",
        )
        val entry = row.toHistoryEntryOrNull()!!
        assertEquals("legacy-lesson", entry.contentId)
        assertEquals("2025-04-01", entry.deliveredDate)
        assertEquals(ContentCategory.MOON_AND_SEASONAL, entry.category)
        assertEquals(ContentCategory.PROTECTION, (row + ("category" to "Protection")).toHistoryEntryOrNull()!!.category)
    }

    @Test
    fun `unknown content type returns null instead of throwing`() {
        assertNull((lesson + ("type" to "ritual")).toContentOrNull())
        assertNull((lesson + ("type" to "UNKNOWN")).toContentOrNull())
    }

    @Test
    fun `unknown tier or missing required fields return null instead of throwing`() {
        assertNull((lesson + ("tier" to "platinum")).toContentOrNull())
        assertNull((lesson - "type").toContentOrNull())
        assertNull((lesson - "tier").toContentOrNull())
        assertNull((lesson - "id").toContentOrNull())
        assertNull((lesson + ("type" to 42)).toContentOrNull())
    }

    @Test
    fun `status never throws - only published is published, a missing status means published`() {
        assertEquals(ContentStatus.DRAFT, (lesson + ("status" to "Publish")).toContentOrNull()?.status)
        assertEquals(ContentStatus.DRAFT, (lesson + ("status" to "archived")).toContentOrNull()?.status)
        assertEquals(ContentStatus.DRAFT, (lesson + ("status" to "draft")).toContentOrNull()?.status)
        assertEquals(ContentStatus.PUBLISHED, (lesson + ("status" to " Published ")).toContentOrNull()?.status)
        assertEquals(ContentStatus.PUBLISHED, (lesson - "status").toContentOrNull()?.status)
    }

    @Test
    fun `malformed optional fields do not reject otherwise readable content`() {
        val content = (lesson + ("tags" to listOf("moon", 7, null)) + ("citations" to "not-a-list")).toContentOrNull()
        assertEquals(listOf("moon"), content?.tags)
        assertNull(content?.citation)
    }

    @Test
    fun `cached row with an unknown type or status never throws`() {
        val cached = lesson.toContentOrNull()!!.toCachedEntity("2026-10-06", "user-1", "Europe/London")
        assertNull(cached.copy(type = "RITUAL").toDomainOrNull())
        assertNull(cached.copy(tier = "").toDomainOrNull())
        assertNull(cached.copy(type = "UNKNOWN").toDomainOrNull())
        assertEquals(ContentStatus.DRAFT, cached.copy(status = "Publish").toDomainOrNull()?.status)
    }

    @Test
    fun `history rows with an unknown type or missing fields are skipped, never thrown`() {
        val row = mapOf<String, Any?>(
            "contentId" to "lesson-1",
            "title" to "Seasonal observation",
            "type" to "Spell",
            "deliveredDate" to "2026-11-15",
        )
        assertEquals(ContentType.SPELL, row.toHistoryEntryOrNull()?.type)
        assertNull((row + ("type" to "ritual")).toHistoryEntryOrNull())
        assertNull((row + ("type" to "unknown")).toHistoryEntryOrNull())
        assertNull((row - "type").toHistoryEntryOrNull())
        assertNull((row - "contentId").toHistoryEntryOrNull())
        assertNull((row - "deliveredDate").toHistoryEntryOrNull())

        val entries = listOf(
            row + ("contentId" to "ritual-1") + ("type" to "ritual"),
            row,
            emptyMap(),
        ).toHistoryEntries()
        assertEquals(listOf("lesson-1"), entries.map { it.contentId })
    }

    @Test
    fun `status wire values still parse under a Turkish default locale`() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))
        try {
            val content = lesson.toContentOrNull()!!
            assertEquals(ContentStatus.PUBLISHED, content.status)
        } finally {
            Locale.setDefault(previous)
        }
    }
}
