package com.jaimesorrow.wiccan.ui.legal

import com.jaimesorrow.wiccan.domain.model.PolicyVersions
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
}
