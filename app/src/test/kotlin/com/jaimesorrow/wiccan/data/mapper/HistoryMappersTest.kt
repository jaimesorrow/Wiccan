package com.jaimesorrow.wiccan.data.mapper

import com.jaimesorrow.wiccan.domain.model.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryMappersTest {

    private val row = mapOf(
        "contentId" to "content-1",
        "title" to "Candle of Clear Intent",
        "type" to "spell",
        "tradition" to "European folk magic",
        "category" to "MoonAndSeasonal",
        "deliveredDate" to "2026-10-06",
    )

    @Test
    fun `a complete history row maps to an entry`() {
        val entry = row.toHistoryEntry()
        assertEquals("content-1", entry.contentId)
        assertEquals(ContentType.SPELL, entry.type)
    }

    @Test
    fun `a legacy row without type is skipped instead of breaking the stream`() {
        assertNull((row - "type").toHistoryEntryOrNull())
    }
}
