package com.jaimesorrow.wiccan.ui.settings

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class LocaleResourcesTest {
    private val resources = File("src/main/res").takeIf(File::exists) ?: File("app/src/main/res")

    @Test
    fun `locale config advertises every selectable language`() {
        val text = resources.resolve("xml/locales_config.xml").readText()
        assertTrue(text.contains("android:name=\"es\""))
        assertTrue(text.contains("android:name=\"pt-BR\""))
        assertEquals(setOf("", "es", "pt-BR"), AppLanguage.entries.map { it.languageTag }.toSet())
    }

    @Test
    fun `Spanish and Brazilian Portuguese keys and placeholders stay in parity`() {
        val base = strings("values/strings.xml")
        val spanish = strings("values-es/strings.xml")
        val portuguese = strings("values-pt-rBR/strings.xml")

        assertEquals(spanish.keys, portuguese.keys)
        listOf(spanish, portuguese).forEach { localized ->
            assertTrue("Locale contains no translated strings", localized.isNotEmpty())
            localized.forEach { (key, value) ->
                assertTrue("Unknown string key: $key", key in base)
                assertEquals("Placeholder mismatch for $key", placeholders(base.getValue(key)), placeholders(value))
            }
        }
        val required = setOf(
            "settings_language",
            "settings_language_hint",
            "settings_language_system",
            "settings_language_spanish",
            "settings_language_portuguese_brazil",
            "content_lessons_english_notice",
        )
        assertTrue(spanish.keys.containsAll(required))
        assertTrue(portuguese.keys.containsAll(required))
    }

    private fun strings(path: String): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resources.resolve(path))
        return (0 until document.getElementsByTagName("string").length).associate { index ->
            val element = document.getElementsByTagName("string").item(index) as Element
            element.getAttribute("name") to element.textContent
        }
    }

    private fun placeholders(value: String): List<String> =
        Regex("%\\d+\\$[sdf]").findAll(value).map { it.value }.toList().sorted()
}
