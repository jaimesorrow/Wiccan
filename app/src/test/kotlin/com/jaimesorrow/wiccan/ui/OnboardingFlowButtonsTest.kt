package com.jaimesorrow.wiccan.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.jaimesorrow.wiccan.analytics.AnalyticsConsentManager
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.ui.archive.ARCHIVE_DETAIL_CONTENT_ID_ARG
import com.jaimesorrow.wiccan.ui.archive.ArchiveDetailScreen
import com.jaimesorrow.wiccan.ui.archive.ArchiveDetailViewModel
import com.jaimesorrow.wiccan.ui.archive.FakeArchiveRepository
import com.jaimesorrow.wiccan.ui.auth.FakeAuthRepository
import com.jaimesorrow.wiccan.ui.auth.OnboardingScreen
import com.jaimesorrow.wiccan.ui.auth.OnboardingViewModel
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentId
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentScreen
import com.jaimesorrow.wiccan.ui.legal.WiccanLegalDocuments
import com.jaimesorrow.wiccan.ui.paywall.FakeSubscriptionRepository
import com.jaimesorrow.wiccan.ui.paywall.PaywallScreen
import com.jaimesorrow.wiccan.ui.paywall.PaywallViewModel
import com.jaimesorrow.wiccan.ui.policy.PolicyAcceptanceScreen
import com.jaimesorrow.wiccan.ui.policy.PolicyAcceptanceViewModel
import com.jaimesorrow.wiccan.ui.policy.PolicyAgreement
import com.jaimesorrow.wiccan.ui.policy.labelRes
import com.wiccan.app.R
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Clicks every button on the pre-app screens (onboarding, policy gate,
 * paywall) and the secondary screens' Back buttons, using the real
 * ViewModels backed by fakes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class OnboardingFlowButtonsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val auth = FakeAuthRepository()
    private val clicks = mutableListOf<String>()

    private fun click(text: String) {
        val node = composeRule.onNode(hasText(text) and hasClickAction())
        // Pinned buttons (Paywall actions, detail Back) are not inside a scroll container.
        runCatching { node.performScrollTo() }
        node.performClick()
        composeRule.waitForIdle()
    }

    // ---- Policy acceptance gate ----

    private fun launchPolicyGate(): AnalyticsConsentManager {
        val consent: AnalyticsConsentManager = mockk(relaxed = true)
        every { consent.consent } returns flowOf(false)
        val viewModel = PolicyAcceptanceViewModel(auth, consent)
        composeRule.setContent {
            MaterialTheme {
                PolicyAcceptanceScreen(
                    viewModel = viewModel,
                    onOpenLegalDocument = { clicks += it.name },
                    onAccountClick = { clicks += "account" },
                )
            }
        }
        return consent
    }

    @Test
    fun `policy gate links open Terms, Privacy and Content & Safety, and Account opens`() {
        launchPolicyGate()
        composeRule.onNodeWithTag("policy-logo").assertExists()
        click("Terms of Service")
        click("Privacy Policy")
        click("Content & Safety")
        click("Account & support / Delete Account")
        assertEquals(
            listOf(LegalDocumentId.TERMS.name, LegalDocumentId.PRIVACY.name, LegalDocumentId.CONTENT_SAFETY.name, "account"),
            clicks,
        )
    }

    @Test
    fun `Continue stays disabled until all four agreements are checked, then accepts`() {
        val consent = launchPolicyGate()
        val continueButton = composeRule.onNode(hasText("Continue") and hasClickAction())
        PolicyAgreement.entries.forEach { agreement ->
            continueButton.performScrollTo().assertIsNotEnabled()
            click(composeRule.activity.getString(agreement.labelRes()))
        }
        continueButton.performScrollTo().assertIsEnabled().performClick()
        composeRule.waitForIdle()
        assertEquals(1, auth.acceptTermsCallCount)
        coVerify { consent.setConsent(false) }
    }

    @Test
    fun `optional analytics box is recorded when checked and never required`() {
        val consent = launchPolicyGate()
        PolicyAgreement.entries.forEach { click(composeRule.activity.getString(it.labelRes())) }
        click(composeRule.activity.getString(R.string.policy_share_analytics))
        click("Continue")
        coVerify { consent.setConsent(true) }
        assertEquals(1, auth.acceptTermsCallCount)
    }

    // ---- Onboarding (verification + profile) ----

    @Test
    fun `verification screen buttons sign out, open account and privacy`() {
        val viewModel = OnboardingViewModel(auth)
        composeRule.setContent {
            MaterialTheme {
                OnboardingScreen(
                    verification = true,
                    viewModel = viewModel,
                    onOpenLegalDocument = { clicks += it.name },
                    onAccountClick = { clicks += "account" },
                )
            }
        }
        composeRule.onNodeWithText("I've verified my email").assertExists()
        composeRule.onNodeWithTag("onboarding-logo").assertExists()
        composeRule.onNodeWithText("Resend verification email").assertExists()
        click("Account & support / Delete Account")
        click("Privacy Policy")
        click("Sign out")
        assertEquals(listOf("account", LegalDocumentId.PRIVACY.name), clicks)
        assertEquals(1, auth.signOutCallCount)
    }

    @Test
    fun `profile screen Continue submits the entered names`() {
        val viewModel = OnboardingViewModel(auth)
        composeRule.setContent {
            MaterialTheme { OnboardingScreen(verification = false, viewModel = viewModel) }
        }
        composeRule.onNodeWithTag("onboarding-logo").assertExists()
        composeRule.onNodeWithText("First name").performTextInput("Rowan")
        composeRule.onNodeWithText("Last name").performTextInput("Wilde")
        click("Continue")
        assertEquals("Rowan", viewModel.state.value.firstName)
        assertEquals("Wilde", viewModel.state.value.lastName)
    }

    // ---- Paywall ----

    @Test
    fun `paywall Account, Terms, Privacy and Restore Purchases all respond`() {
        val subscriptions = FakeSubscriptionRepository()
        val viewModel = PaywallViewModel(subscriptions, mockk(relaxed = true))
        composeRule.setContent {
            MaterialTheme {
                PaywallScreen(
                    viewModel = viewModel,
                    onAccountClick = { clicks += "account" },
                    onTermsClick = { clicks += "terms" },
                    onPrivacyClick = { clicks += "privacy" },
                )
            }
        }
        composeRule.onNodeWithTag("paywall-logo").assertExists()
        click("Account & support")
        click("Terms")
        click("Privacy")
        click("Restore Purchases")
        assertEquals(listOf("account", "terms", "privacy"), clicks)
        assertEquals(1, subscriptions.restoreCallCount)
    }

    // ---- Secondary screens' Back buttons ----

    @Test
    fun `legal document Back returns`() {
        composeRule.setContent {
            MaterialTheme {
                LegalDocumentScreen(document = WiccanLegalDocuments.terms, onBack = { clicks += "back" })
            }
        }
        composeRule.onNodeWithTag("legal-logo").assertExists()
        click("← Back")
        assertEquals(listOf("back"), clicks)
    }

    @Test
    fun `Grimoire entry Back returns`() {
        val archive = FakeArchiveRepository()
        archive.contentResults["lesson-42"] = AppResult.Failure(AppError.NotFound)
        val viewModel = ArchiveDetailViewModel(SavedStateHandle(mapOf(ARCHIVE_DETAIL_CONTENT_ID_ARG to "lesson-42")), archive)
        composeRule.setContent {
            MaterialTheme { ArchiveDetailScreen(onBack = { clicks += "back" }, viewModel = viewModel) }
        }
        click("← Return to your Grimoire")
        assertEquals(listOf("back"), clicks)
    }
}
