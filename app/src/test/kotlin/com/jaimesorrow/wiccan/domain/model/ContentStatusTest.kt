package com.jaimesorrow.wiccan.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ContentStatusTest {
    @Test
    fun `known values parse case insensitively`() {
        assertEquals(ContentStatus.PUBLISHED, ContentStatus.fromString("published"))
        assertEquals(ContentStatus.PUBLISHED, ContentStatus.fromString(" PUBLISHED "))
        assertEquals(ContentStatus.DRAFT, ContentStatus.fromString("draft"))
        assertEquals(ContentStatus.DRAFT, ContentStatus.fromString("Draft"))
    }

    @Test
    fun `v2 editorial statuses are never treated as published`() {
        listOf("review", "rejected", "archived").forEach { value ->
            assertEquals("value=$value", ContentStatus.DRAFT, ContentStatus.fromString(value))
        }
    }

    @Test
    fun `unknown, null and blank values default to DRAFT without throwing`() {
        listOf(null, "", "  ", "Publish", "live", "ritual").forEach { value ->
            assertEquals("value=$value", ContentStatus.DRAFT, ContentStatus.fromString(value))
        }
    }
}
