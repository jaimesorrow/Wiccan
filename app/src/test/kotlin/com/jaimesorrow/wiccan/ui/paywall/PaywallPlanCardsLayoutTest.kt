package com.jaimesorrow.wiccan.ui.paywall

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.jaimesorrow.wiccan.domain.model.SubscriptionPlan
import com.jaimesorrow.wiccan.ui.theme.LocallyResolvableTypography
import com.jaimesorrow.wiccan.ui.theme.WiccanPalette
import com.jaimesorrow.wiccan.ui.theme.toColorScheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot and geometry checks for the v1.1 plan picker. On a 411x891 phone
 * both plan cards must sit fully inside the visible scroll area without any
 * scrolling, above the sticky checkout footer, at 100%, 130% and 150% text.
 * On a small 320x533 screen each card must scroll fully into view (never
 * clipped by the footer) while the footer names the selected plan.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class)
class PaywallPlanCardsLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val bothPlans = PaywallUiState(
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
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun `phone 411x891 shows both plans above the fold at 100 percent text`() = aboveTheFold(1.0f)

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun `phone 411x891 shows both plans above the fold at 130 percent text`() = aboveTheFold(1.3f)

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun `phone 411x891 shows both plans above the fold at 150 percent text`() = aboveTheFold(1.5f)

    @Test
    @Config(qualifiers = "w320dp-h533dp-xhdpi")
    fun `small 320x533 keeps both plans readable at 100 percent text`() = readableOnSmallScreen(1.0f)

    @Test
    @Config(qualifiers = "w320dp-h533dp-xhdpi")
    fun `small 320x533 keeps both plans readable at 130 percent text`() = readableOnSmallScreen(1.3f)

    @Test
    @Config(qualifiers = "w320dp-h533dp-xhdpi")
    fun `small 320x533 keeps both plans readable at 150 percent text`() = readableOnSmallScreen(1.5f)

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun `payment recovery and winback terms are visible`() {
        render(
            fontScale = 1.0f,
            dark = false,
            state = bothPlans.copy(
                selectedPlan = SubscriptionPlan.MONTHLY,
                subscriptionOnHold = true,
                monthlyWinBackPrice = "$2.99",
            ),
        )

        composeRule.onNodeWithTag("payment-on-hold").assertIsDisplayed()
        composeRule.onNodeWithTag("winback-offer").performScrollTo().assertIsDisplayed()
    }

    private fun aboveTheFold(fontScale: Float) {
        render(fontScale, dark = fontScale == 1.3f)
        val viewport = bounds("paywall-scroll")
        assertFullyInside("yearly card", bounds("paywall-plan-yearly"), viewport)
        assertFullyInside("monthly card", bounds("paywall-plan-monthly"), viewport)
        composeRule.onNodeWithTag("paywall-plan-yearly").assertIsDisplayed()
        composeRule.onNodeWithTag("paywall-plan-monthly").assertIsDisplayed()
        assertFooterNamesPlan(viewport)
        composeRule.onRoot().captureRoboImage(filePath = "$OUT/wiccan-paywall-411x891-${pct(fontScale)}.png")
    }

    private fun readableOnSmallScreen(fontScale: Float) {
        render(fontScale, dark = fontScale == 1.3f)
        val viewport = bounds("paywall-scroll")
        assertFooterNamesPlan(viewport)
        for (tag in listOf("paywall-plan-yearly", "paywall-plan-monthly")) {
            composeRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
            composeRule.waitForIdle()
            assertFullyInside(tag, bounds(tag), viewport)
            composeRule.onRoot().captureRoboImage(
                filePath = "$OUT/wiccan-paywall-320x533-${pct(fontScale)}-${tag.removePrefix("paywall-plan-")}.png",
            )
        }
    }

    private fun assertFooterNamesPlan(viewport: Rect) {
        composeRule.onNodeWithTag("paywall-selected-terms").assertIsDisplayed()
        composeRule.onNodeWithTag("paywall-subscribe").assertIsDisplayed()
        val footer = bounds("paywall-footer")
        assertTrue("Sticky footer must sit below the scroll area", footer.top >= viewport.bottom - 0.5f)
        assertTrue("Sticky footer must fit on screen", footer.bottom <= bounds(null).bottom + 0.5f)
    }

    private fun render(fontScale: Float, dark: Boolean, state: PaywallUiState = bothPlans) {
        composeRule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                MaterialTheme(
                    colorScheme = (if (dark) WiccanPalette.ROYAL_PLUM else WiccanPalette.LIGHT_IVORY).toColorScheme(),
                    typography = LocallyResolvableTypography,
                ) {
                    PaywallContent(
                        state = state,
                        insets = PaddingValues(0.dp),
                        onAccountClick = {},
                        onTermsClick = {},
                        onPrivacyClick = {},
                        onEvent = {},
                        onSubscribe = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun bounds(tag: String?): Rect =
        (if (tag == null) composeRule.onRoot() else composeRule.onNodeWithTag(tag))
            .fetchSemanticsNode().boundsInRoot

    private fun assertFullyInside(name: String, inner: Rect, outer: Rect) {
        val slack = 0.5f
        assertTrue(
            "$name $inner must be fully visible inside $outer (not clipped)",
            inner.top >= outer.top - slack && inner.bottom <= outer.bottom + slack &&
                inner.left >= outer.left - slack && inner.right <= outer.right + slack,
        )
    }

    private fun pct(fontScale: Float) = (fontScale * 100).toInt()

    private companion object {
        const val OUT = "build/outputs/roborazzi"
    }
}
