package com.jaimesorrow.wiccan.ui.legal

import com.jaimesorrow.wiccan.billing.TermsBasePrices
import com.jaimesorrow.wiccan.core.config.PolicyUrls
import com.jaimesorrow.wiccan.domain.model.PolicyVersions
import com.jaimesorrow.wiccan.domain.model.SubscriptionPlan
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
        assertTrue(WiccanLegalDocuments.potionSafety.effectiveDate.isNotBlank())
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
    fun `terms v7 list both plans with their trial renewal and cancel terms`() {
        assertEquals(7, PolicyVersions.TERMS)
        assertEquals(7, PolicyVersions.PRIVACY)
        val text = WiccanLegalDocuments.terms.auditText()
        // Prices come from the single TermsBasePrices constant, so a price change fails here until the Terms change.
        val monthly = TermsBasePrices.usdText(SubscriptionPlan.MONTHLY)
        val yearly = TermsBasePrices.usdText(SubscriptionPlan.YEARLY)
        assertTrue(text.contains("monthly plan with a US base price of $monthly per month"))
        assertTrue(text.contains("yearly plan with a US base price of $yearly per year"))
        assertTrue(text.contains("free trial at the length shown at checkout (7 days on the monthly plan, 14 days on the yearly plan)"))
        assertTrue(text.contains("Google Play charges the yearly price once a year until you cancel"))
        assertTrue(text.contains("cancel either plan at any time through Google Play"))
        val privacy = WiccanLegalDocuments.privacy.auditText()
        assertTrue(privacy.contains("a reminder before the trial ends"))
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
    fun `potion safety covers ingestion exposure storage and emergencies`() {
        val text = WiccanLegalDocuments.potionSafety.auditText()
        listOf(
            "Never ingest essential oils",
            "patch test",
            "pregnancy or nursing",
            "children and animals",
            "Ritual use only",
            "Poison Control",
            "does not override a stricter warning",
            "does not promise or guarantee",
        ).forEach { required -> assertTrue("Missing potion safety guidance: $required", text.contains(required, true)) }
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
            "NOAA",
            "station identifier",
        ).forEach { required -> assertTrue("Missing privacy disclosure: $required", text.contains(required, true)) }
    }

    @Test
    fun `safety guidance rejects hazardous and professional uses`() {
        val text = WiccanLegalDocuments.contentSafety.auditText()
        listOf("ingest", "burn", "poison control", "emergency", "consent", "automated and AI review")
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
            "potion-safety" to WiccanLegalDocuments.potionSafety,
        ).forEach { (page, document) ->
            val expected = buildList {
                add(document.title)
                add("Effective ${document.effectiveDate}" + (document.version?.let { " · Version $it" } ?: ""))
                add(document.introduction)
                document.sections.forEach { section ->
                    add(section.heading)
                    addAll(section.paragraphs)
                }
            }.joinToString(" ").normalized()
            assertEquals("hosting/$page.html <main> differs from the in-app text", expected, hostedMainText(page))
        }
    }

    @Test
    fun `every public policy URL has a hosted page`() {
        listOf(
            PolicyUrls.TERMS_OF_SERVICE,
            PolicyUrls.PRIVACY_POLICY,
            PolicyUrls.POTION_SAFETY,
            PolicyUrls.ACCOUNT_DELETION_URL,
        ).forEach { url ->
            val page = url.substringAfterLast('/')
            assertTrue("No hosted page for $url", hostingFile(page).isFile)
        }
        val deletion = hostedText("delete-account")
        assertTrue(deletion.contains(PolicyUrls.SUPPORT_EMAIL))
        assertTrue(deletion.contains(OPERATOR))
    }

    private fun hostingFile(name: String): File =
        listOf(File("../hosting", name), File("hosting", name)).firstOrNull { it.exists() } ?: File("../hosting", name)

    private fun hostedMainText(page: String): String {
        val html = hostingFile("$page.html").readText()
        val main = Regex("<main>(.*)</main>", RegexOption.DOT_MATCHES_ALL).find(html)?.groupValues?.get(1)
            ?: error("hosting/$page.html has no <main>")
        return main.decodedText()
    }

    private fun hostedText(page: String): String = hostingFile("$page.html").readText().decodedText()

    private fun String.decodedText(): String = this
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
