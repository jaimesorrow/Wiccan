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
        assertTrue(text.contains("android:name=\"en\""))
        assertTrue(text.contains("android:name=\"es\""))
        assertTrue(text.contains("android:name=\"pt-BR\""))
        assertEquals(setOf("", "en", "es", "pt-BR"), AppLanguage.entries.map { it.languageTag }.toSet())
    }

    @Test
    fun `Spanish and Brazilian Portuguese keys and placeholders stay in parity`() {
        val base = translatableStrings("values/strings.xml")
        val spanish = translatableStrings("values-es/strings.xml")
        val portuguese = translatableStrings("values-pt-rBR/strings.xml")

        assertEquals(
            "Spanish is missing: ${base.keys - spanish.keys}; extra: ${spanish.keys - base.keys}",
            base.keys,
            spanish.keys,
        )
        assertEquals(
            "Portuguese is missing: ${base.keys - portuguese.keys}; extra: ${portuguese.keys - base.keys}",
            base.keys,
            portuguese.keys,
        )
        listOf("es" to spanish, "pt-BR" to portuguese).forEach { (locale, localized) ->
            assertTrue("$locale contains no translated strings", localized.isNotEmpty())
            localized.forEach { (key, value) ->
                assertEquals(
                    "Placeholder mismatch for $locale/$key",
                    placeholders(base.getValue(key)),
                    placeholders(value),
                )
            }
        }
        val required = setOf(
            "settings_language",
            "settings_language_hint",
            "settings_language_system",
            "settings_language_english",
            "settings_language_spanish",
            "settings_language_portuguese_brazil",
            "content_lessons_english_notice",
        )
        assertTrue(spanish.keys.containsAll(required))
        assertTrue(portuguese.keys.containsAll(required))
    }

    private fun translatableStrings(path: String): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resources.resolve(path))
        return (0 until document.getElementsByTagName("string").length).mapNotNull { index ->
            val element = document.getElementsByTagName("string").item(index) as Element
            if (element.getAttribute("translatable") == "false") {
                null
            } else {
                element.getAttribute("name") to element.textContent
            }
        }.toMap()
    }

    private fun placeholders(value: String): List<String> =
        Regex("%\\d+\\$[sdf]|%s|%d|%f").findAll(value).map { it.value }.toList().sorted()
}
