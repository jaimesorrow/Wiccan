package com.jaimesorrow.wiccan.ui.paywall

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.jaimesorrow.wiccan.analytics.AnalyticsLogger
import com.jaimesorrow.wiccan.domain.model.SubscriptionOffer
import com.jaimesorrow.wiccan.domain.model.SubscriptionPlan
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose coverage for the v1.1 plan cards: yearly leads when Play has both
 * valid plans, and a missing yearly offer keeps the 1.0 monthly-only paywall.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class PaywallScreenPlanCardsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val yearly = SubscriptionOffer(
        "$39.99",
        true,
        "yearly-token",
        SubscriptionPlan.YEARLY,
        39_990_000L,
        "USD",
        freeTrialDays = 14,
    )

    @Test
    fun `both yearly and monthly cards render and yearly is preselected`() {
        launch(yearlyOffer = yearly)

        composeRule.onNodeWithText("Choose your plan").performScrollTo().assertIsDisplayed()

        // The plan cards live in a scrollable column above a fixed checkout footer,
        // so assert each card (and its copy) after scrolling it into view.
        composeRule.onNodeWithTag("paywall-plan-yearly").performScrollTo().assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithText("Yearly").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Best value").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("$39.99 / year").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("paywall-plan-monthly").performScrollTo().assertIsDisplayed().assertIsNotSelected()
        composeRule.onNodeWithText("Monthly").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("$5.99 / month").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `without a valid yearly plan the paywall stays on the monthly-only flow`() {
        // getYearlyOffer() is null when Play has no yearly product or the offer is invalid.
        // PlanChoice is hidden; the unchanged 1.0 typography and a monthly checkout remain.
        val viewModel = launch(yearlyOffer = null)

        assertFalse(viewModel.state.value.hasYearlyPlan)
        assertEquals(SubscriptionPlan.MONTHLY, viewModel.state.value.selectedPlan)
        composeRule.onNodeWithTag("paywall-plan-yearly").assertDoesNotExist()
        composeRule.onNodeWithTag("paywall-plan-monthly").assertDoesNotExist()
        composeRule.onNodeWithText("Choose your plan").assertDoesNotExist()
        composeRule.onNodeWithText("Best value").assertDoesNotExist()
        composeRule.onNodeWithText("Yearly").assertDoesNotExist()
        composeRule.onNodeWithText("7 days free").assertIsDisplayed()
        composeRule.onNodeWithText("Then $5.99 per month. Auto-renews until canceled.").assertIsDisplayed()
    }

    @Test
    fun `trial wording uses the offer period for each plan`() {
        launch(yearlyOffer = yearly)

        composeRule.onNodeWithText("Yearly: 14 days free, then $39.99/year").assertIsDisplayed()
        composeRule.onNodeWithText("Start My 14-Day Free Trial").assertIsDisplayed()
        composeRule.onNodeWithText(
            "No charge during the 14-day trial. Google Play charges $39.99 yearly after the trial unless canceled before it ends. Cancel anytime through Google Play.",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("14-day free trial", substring = true).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("paywall-plan-monthly").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Monthly: 7 days free, then $5.99/month").assertIsDisplayed()
        composeRule.onNodeWithText("Start My 7-Day Free Trial").assertIsDisplayed()
        composeRule.onNodeWithText("7-day free trial", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `winback checkout names the promotional period and renewal price`() {
        val repository = FakeSubscriptionRepository().apply {
            hasFreeTrial = false
            monthlyWinBackPrice = "$2.99"
        }
        launch(repository)

        composeRule.onNodeWithText("Monthly: $2.99 for the first month, then $5.99/month").assertIsDisplayed()
        composeRule.onNodeWithText("Auto-renews until canceled. Cancel anytime in Google Play.").assertIsDisplayed()
    }

    @Test
    fun `without a winback offer the regular checkout terms stay in place`() {
        val repository = FakeSubscriptionRepository().apply {
            hasFreeTrial = false
            yearlyOffer = yearly.copy(hasFreeTrial = false)
        }
        launch(repository)

        composeRule.onNodeWithTag("winback-offer").assertDoesNotExist()
        composeRule.onNodeWithText("Yearly: $39.99/year").assertIsDisplayed()
        composeRule.onNodeWithText("Billed $39.99 every year. Auto-renews until canceled.").assertIsDisplayed()
        composeRule.onNodeWithTag("paywall-plan-monthly").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("winback-offer").assertDoesNotExist()
        composeRule.onNodeWithText("Monthly: $5.99/month").assertIsDisplayed()
        composeRule.onNodeWithText("Billed $5.99 every month. Auto-renews until canceled.").assertIsDisplayed()
    }

    @Test
    fun `winback terms follow the selected plan and disappear when that plan has none`() {
        val repository = FakeSubscriptionRepository().apply {
            hasFreeTrial = false
            yearlyOffer = yearly.copy(
                hasFreeTrial = false,
                isWinBack = true,
                winBackFormattedPrice = "$19.99",
            )
        }
        launch(repository)

        composeRule.onNodeWithTag("winback-offer").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Return for $19.99 for the first billing period, then the regular Google Play price.")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Yearly: $19.99 for the first year, then $39.99/year").assertIsDisplayed()

        composeRule.onNodeWithTag("paywall-plan-monthly").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("winback-offer").assertDoesNotExist()
        composeRule.onNodeWithText("Monthly: $5.99/month").assertIsDisplayed()
    }

    @Test
    fun `a resume refresh keeps the plans and the scroll position`() {
        val repository = FakeSubscriptionRepository().apply { this.yearlyOffer = yearly }
        val viewModel = launch(repository)
        composeRule.onNodeWithTag("paywall-plan-monthly").performScrollTo()
        composeRule.onNodeWithTag("paywall-scroll").performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        val scrolledTo = scrollOffset()
        assertTrue("the screen should have scrolled", scrolledTo > 0f)

        val gate = CompletableDeferred<Unit>()
        repository.offerGate = gate
        composeRule.runOnIdle { viewModel.refreshOffer() }
        composeRule.waitForIdle()

        // Mid-refresh: both cards are still there, nothing jumped back to the top.
        composeRule.onNodeWithTag("paywall-plan-yearly").assertExists()
        composeRule.onNodeWithTag("paywall-plan-monthly").assertExists()
        assertEquals(scrolledTo, scrollOffset(), 0.5f)

        gate.complete(Unit)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("paywall-plan-yearly").assertExists()
        assertEquals(scrolledTo, scrollOffset(), 0.5f)
    }

    private fun scrollOffset(): Float = composeRule.onNodeWithTag("paywall-scroll")
        .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

    private fun launch(yearlyOffer: SubscriptionOffer?): PaywallViewModel =
        launch(FakeSubscriptionRepository().apply { this.yearlyOffer = yearlyOffer })

    private fun launch(repository: FakeSubscriptionRepository): PaywallViewModel {
        val viewModel = PaywallViewModel(repository, mockk<AnalyticsLogger>(relaxed = true))
        composeRule.setContent {
            MaterialTheme {
                PaywallScreen(viewModel = viewModel)
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { !viewModel.state.value.isLoadingPrice }
        composeRule.waitForIdle()
        return viewModel
    }
}
