package com.jaimesorrow.wiccan.ui.legal

import com.jaimesorrow.wiccan.core.config.PolicyUrls
import com.jaimesorrow.wiccan.domain.model.PolicyVersions
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalDocumentsTest {
    @Test
    fun `versioned native documents match the acceptance gate`() {
        assertEquals(PolicyVersions.TERMS, WiccanLegalDocuments.terms.version)
        assertEquals(PolicyVersions.PRIVACY, WiccanLegalDocuments.privacy.version)
        assertTrue(WiccanLegalDocuments.terms.effectiveDate.isNotBlank())
        assertTrue(WiccanLegalDocuments.privacy.effectiveDate.isNotBlank())
    }

    @Test
    fun `terms disclose recurring billing cancellation and content limits`() {
        val text = WiccanLegalDocuments.terms.auditText()
        assertTrue(text.contains("automatically renewing", ignoreCase = true))
        assertTrue(text.contains("Google Play", ignoreCase = true))
        assertTrue(text.contains("medical", ignoreCase = true))
        assertTrue(text.contains("AI assistance", ignoreCase = true))
    }

    @Test
    fun `terms and safety guidance explicitly disclaim any promised result`() {
        val terms = WiccanLegalDocuments.terms
        val section = terms.sections.single { it.heading == "No promise or guarantee of results" }.paragraphs.joinToString(" ")
        listOf(
            "does not promise, represent, warrant, or guarantee",
            "spell, potion, charm, ritual, ceremony",
            "supernatural, paranormal, magical, psychic, or spiritual effect",
            "not a statement of fact, prediction, or promise",
            "absence of any result is not a defect",
        ).forEach { required -> assertTrue("Missing no-results disclaimer: $required", section.contains(required)) }
        assertTrue(WiccanLegalDocuments.contentSafety.auditText().contains("does not promise or guarantee"))
    }

    @Test
    fun `privacy covers collected data processors and deletion retention`() {
        val text = WiccanLegalDocuments.privacy.auditText()
        listOf(
            "email address",
            "push token",
            "purchase data",
            "Analytics",
            "Crashlytics",
            "does not sell",
            "pseudonymous",
            "Delete Account",
        ).forEach { required -> assertTrue("Missing privacy disclosure: $required", text.contains(required, true)) }
    }

    @Test
    fun `safety guidance rejects hazardous and professional uses`() {
        val text = WiccanLegalDocuments.contentSafety.auditText()
        listOf("ingest", "burn", "poison control", "emergency", "consent", "human")
            .forEach { required -> assertTrue("Missing safety guidance: $required", text.contains(required, true)) }
    }

    @Test
    fun `policies identify the operator adult audience and contact`() {
        val terms = WiccanLegalDocuments.terms.auditText()
        val privacy = WiccanLegalDocuments.privacy.auditText()
        listOf(terms, privacy).forEach { text ->
            assertTrue(text.contains(OPERATOR))
            assertTrue(text.contains(PolicyUrls.SUPPORT_EMAIL))
            assertTrue(text.contains(PolicyUrls.ACCOUNT_DELETION_URL))
        }
        assertTrue(terms.contains("at least 18 years old"))
        assertTrue(privacy.contains("advertising ID"))
        assertTrue(privacy.contains("collected only if you opt in"))
        assertTrue(privacy.contains("your consent, for usage analytics and crash reports"))
    }

    @Test
    fun `hosted pages mirror the native documents word for word`() {
        mapOf(
            "terms" to WiccanLegalDocuments.terms,
            "privacy" to WiccanLegalDocuments.privacy,
            "content-safety" to WiccanLegalDocuments.contentSafety,
        ).forEach { (page, document) ->
            val hosted = hostedText(page)
            val expected = buildList {
                add(document.title)
                add("Effective ${document.effectiveDate}")
                document.version?.let { add("Version $it") }
                add(document.introduction)
                document.sections.forEach { section ->
                    add(section.heading)
                    addAll(section.paragraphs)
                }
            }
            expected.forEach { line ->
                assertTrue("hosting/$page.html is missing: $line", hosted.contains(line.normalized()))
            }
        }
    }

    @Test
    fun `every public policy URL has a hosted page`() {
        listOf(PolicyUrls.TERMS_OF_SERVICE, PolicyUrls.PRIVACY_POLICY, PolicyUrls.ACCOUNT_DELETION_URL).forEach { url ->
            val page = url.substringAfterLast('/')
            assertTrue("No hosted page for $url", hostingFile("$page.html").isFile)
        }
        val deletion = hostedText("delete-account")
        assertTrue(deletion.contains(PolicyUrls.SUPPORT_EMAIL))
        assertTrue(deletion.contains(OPERATOR))
    }

    private fun hostingFile(name: String): File =
        listOf(File("../hosting", name), File("hosting", name)).firstOrNull { it.exists() } ?: File("../hosting", name)

    private fun hostedText(page: String): String = hostingFile("$page.html").readText()
        .replace(Regex("<[^>]+>"), "")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#x27;", "'")
        .replace("&amp;", "&")
        .normalized()

    private fun String.normalized(): String = trim().replace(Regex("\\s+"), " ")

    private fun LegalDocument.auditText(): String = buildString {
        append(title)
        append(' ')
        append(introduction)
        sections.forEach { section ->
            append(' ')
            append(section.heading)
            section.paragraphs.forEach {
                append(' ')
                append(it)
            }
        }
    }

    private companion object {
        const val OPERATOR = "Jaime Sorrow"
    }
}
