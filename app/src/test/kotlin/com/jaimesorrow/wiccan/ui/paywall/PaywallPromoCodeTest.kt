package com.jaimesorrow.wiccan.ui.paywall

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.jaimesorrow.wiccan.analytics.AnalyticsLogger
import com.jaimesorrow.wiccan.billing.launchPlayPromoRedeem
import com.jaimesorrow.wiccan.domain.model.SubscriptionPlan
import com.jaimesorrow.wiccan.ui.theme.MidnightGrimoireTheme
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class PaywallPromoCodeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val loaded = PaywallUiState(
        isLoadingPrice = false,
        price = "$5.99",
        hasFreeTrial = true,
        freeTrialDays = 7,
        yearlyPrice = "$39.99",
        yearlyHasFreeTrial = true,
        yearlyFreeTrialDays = 14,
        yearlyMonthlyEquivalent = "$3.33",
        yearlySavingsPercent = 44,
        selectedPlan = SubscriptionPlan.YEARLY,
    )

    @Test
    fun `promo toggle shows on subscription onboarding paywall`() {
        mountPaywall(grimoire = true)
        composeRule.onNodeWithTag("paywall-promo-toggle").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `promo toggle shows on in-app paywall`() {
        mountPaywall(grimoire = false)
        composeRule.onNodeWithTag("paywall-promo-toggle").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `redeem stays disabled until the field has text`() {
        mountPaywall()
        composeRule.onNodeWithTag("paywall-promo-toggle").performScrollTo().performClick()
        composeRule.onNodeWithTag("paywall-promo-redeem").assertIsNotEnabled()
        composeRule.onNodeWithTag("paywall-promo-field").performTextInput("WICCANLAUNCH2026")
        composeRule.onNodeWithTag("paywall-promo-redeem").assertIsEnabled()
    }

    @Test
    fun `redeem opens Play without touching subscription verification`() {
        composeRule.setContent {
            MaterialTheme {
                PaywallContent(
                    state = loaded,
                    insets = PaddingValues(),
                    onAccountClick = {},
                    onTermsClick = {},
                    onPrivacyClick = {},
                    onEvent = {},
                    onSubscribe = {},
                    onRedeemPromoCode = { launchPlayPromoRedeem(composeRule.activity, it) },
                )
            }
        }
        composeRule.onNodeWithTag("paywall-promo-toggle").performScrollTo().performClick()
        val field = composeRule.onNodeWithTag("paywall-promo-field")
        field.performScrollTo().performClick()
        field.performTextInput("wiccanlaunch2026")
        val redeem = composeRule.onNodeWithTag("paywall-promo-redeem")
        redeem.performScrollTo().assertIsEnabled().performClick()
        composeRule.waitForIdle()

        val intent = shadowOf(composeRule.activity).nextStartedActivity
        org.junit.Assert.assertNotNull(intent)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(
            "https://play.google.com/redeem?code=WICCANLAUNCH2026",
            intent.data.toString(),
        )
    }

    private fun mountPaywall(grimoire: Boolean = false) {
        composeRule.setContent {
            if (grimoire) {
                MidnightGrimoireTheme {
                    paywallBody()
                }
            } else {
                MaterialTheme {
                    paywallBody()
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun paywallBody() {
        PaywallContent(
            state = loaded,
            insets = PaddingValues(),
            onAccountClick = {},
            onTermsClick = {},
            onPrivacyClick = {},
            onEvent = {},
            onSubscribe = {},
            onRedeemPromoCode = { launchPlayPromoRedeem(composeRule.activity, it) },
        )
    }
}
