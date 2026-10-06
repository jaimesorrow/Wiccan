package com.jaimesorrow.wiccan.data.mapper

import com.jaimesorrow.wiccan.domain.model.ContentCategory
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
        val content = lesson.toContent()
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
            )
            ).toContent()
        assertEquals(content, content.toCachedEntity("2026-10-06", "user-1", "Europe/London").toDomain())
        assertEquals(ContentCategory.PROTECTION, content.category)
    }

    @Test
    fun `legacy difficulty data does not become a field or affect content mapping`() {
        assertEquals(lesson.toContent(), (lesson + ("difficulty" to "advanced")).toContent())
        assertFalse(lesson.toContent().javaClass.declaredFields.any { it.name == "difficulty" })
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
        val entry = row.toHistoryEntry()
        assertEquals("legacy-lesson", entry.contentId)
        assertEquals("2025-04-01", entry.deliveredDate)
        assertEquals(ContentCategory.MOON_AND_SEASONAL, entry.category)
        assertEquals(ContentCategory.PROTECTION, (row + ("category" to "Protection")).toHistoryEntry().category)
    }
}
