package com.jaimesorrow.wiccan.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ContentTypeTest {
    @Test
    fun `known lowercase values parse`() {
        assertEquals(ContentType.SPELL, ContentType.fromString("spell"))
        assertEquals(ContentType.POTION, ContentType.fromString("potion"))
        assertEquals(ContentType.LESSON, ContentType.fromString("lesson"))
    }

    @Test
    fun `parsing is case insensitive and trims whitespace`() {
        assertEquals(ContentType.SPELL, ContentType.fromString("SPELL"))
        assertEquals(ContentType.POTION, ContentType.fromString("Potion"))
        assertEquals(ContentType.LESSON, ContentType.fromString("  lEsSoN \n"))
    }

    @Test
    fun `ritual is not a content type and maps to UNKNOWN`() {
        assertEquals(ContentType.UNKNOWN, ContentType.fromString("ritual"))
        assertEquals(ContentType.UNKNOWN, ContentType.fromString("RITUAL"))
    }

    @Test
    fun `unknown, null and blank values map to UNKNOWN without throwing`() {
        listOf(null, "", "   ", "guided-practice", "spells", "spell potion", "42").forEach { value ->
            assertEquals("value=$value", ContentType.UNKNOWN, ContentType.fromString(value))
        }
    }

    @Test
    fun `the UNKNOWN name itself is not accepted as stored data`() {
        assertEquals(ContentType.UNKNOWN, ContentType.fromString("unknown"))
    }

    @Test
    fun `every real member round trips through its lowercase name`() {
        ContentType.entries.filter { it != ContentType.UNKNOWN }.forEach { type ->
            assertEquals(type, ContentType.fromString(type.name.lowercase()))
        }
    }
}
