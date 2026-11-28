package com.jaimesorrow.wiccan.ui.paywall

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureRoboImage
import com.jaimesorrow.wiccan.domain.model.SubscriptionPlan
import com.jaimesorrow.wiccan.ui.theme.LocallyResolvableTypography
import com.jaimesorrow.wiccan.ui.theme.MidnightGrimoireTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
class PaywallPromoCodeScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

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
    fun `capture paywall with promo field expanded`() {
        composeRule.setContent {
            MidnightGrimoireTheme {
                PaywallContent(
                    state = loaded,
                    insets = PaddingValues(),
                    onAccountClick = {},
                    onTermsClick = {},
                    onPrivacyClick = {},
                    onEvent = {},
                    onSubscribe = {},
                )
            }
        }
        composeRule.onNodeWithTag("paywall-promo-toggle").performScrollTo().performClick()
        composeRule.onNodeWithTag("paywall-promo-field").performTextInput("WICCANLAUNCH2026")
        composeRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/wiccan-paywall-promo-expanded.png",
        )
    }

    @Test
    fun `capture subscription onboarding paywall with promo toggle visible`() {
        composeRule.setContent {
            MaterialTheme(typography = LocallyResolvableTypography) {
                PaywallContent(
                    state = loaded,
                    insets = PaddingValues(),
                    onAccountClick = {},
                    onTermsClick = {},
                    onPrivacyClick = {},
                    onEvent = {},
                    onSubscribe = {},
                )
            }
        }
        composeRule.onNodeWithTag("paywall-promo-toggle").performScrollTo()
        composeRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/wiccan-onboarding-subscription-promo.png",
        )
    }
}
