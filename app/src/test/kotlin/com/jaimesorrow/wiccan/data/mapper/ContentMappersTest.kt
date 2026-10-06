package com.jaimesorrow.wiccan.data.mapper

import com.jaimesorrow.wiccan.domain.model.ContentDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContentMappersTest {
    private val legacyPayload: Map<String, Any?> = mapOf(
        "id" to "lesson-1",
        "type" to "lesson",
        "tier" to "free",
        "category" to "MoonAndSeasonal",
        "title" to "The Waxing Moon",
        "intro" to "A visible cycle.",
        "body" to "A recurring structure for reflection.",
    )

    @Test
    fun `lesson sections survive callable mapping and offline cache round trip`() {
        val content = (
            legacyPayload + mapOf(
                "reflection" to "What should grow slowly?",
                "practice" to "Record one achievable action.",
                "difficulty" to "intermediate",
            )
            ).toContent()

        assertEquals("What should grow slowly?", content.reflection)
        assertEquals("Record one achievable action.", content.practice)
        assertEquals(ContentDifficulty.INTERMEDIATE, content.difficulty)
        assertEquals(content, content.toCachedEntity("2026-10-02").toDomain())
    }

    @Test
    fun `legacy content remains readable without structured lesson fields`() {
        val content = legacyPayload.toContent()
        assertNull(content.reflection)
        assertNull(content.practice)
        assertNull(content.difficulty)
        assertEquals(content, content.toCachedEntity("2026-10-02").toDomain())
    }

    @Test
    fun `unknown reader level does not prevent a lesson from opening`() {
        val content = (legacyPayload + ("difficulty" to "future-level")).toContent()
        assertNull(content.difficulty)
        assertEquals("lesson-1", content.id)
    }
}
