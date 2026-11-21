package com.jaimesorrow.wiccan.ui.paywall

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.jaimesorrow.wiccan.analytics.AnalyticsLogger
import com.jaimesorrow.wiccan.domain.model.SubscriptionOffer
import com.jaimesorrow.wiccan.domain.model.SubscriptionPlan
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private fun launch(yearlyOffer: SubscriptionOffer?): PaywallViewModel {
        val repository = FakeSubscriptionRepository().apply { this.yearlyOffer = yearlyOffer }
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
