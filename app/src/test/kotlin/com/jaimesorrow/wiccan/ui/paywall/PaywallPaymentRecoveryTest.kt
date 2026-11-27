package com.jaimesorrow.wiccan.ui.paywall

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.jaimesorrow.wiccan.analytics.AnalyticsLogger
import com.jaimesorrow.wiccan.domain.model.SubscriptionState
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The on-hold recovery card is shown only for the server-mirrored ON_HOLD
 * state. Grace period and other states keep the card hidden. Tapping the
 * update-payment button opens Play's general subscriptions page.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class PaywallPaymentRecoveryTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `on-hold card is shown only while the subscription is on hold`() {
        val repository = FakeSubscriptionRepository()
        val viewModel = launch(repository)

        composeRule.onNodeWithTag("payment-on-hold").assertDoesNotExist()

        composeRule.runOnIdle { repository.emitSubscriptionState(SubscriptionState.IN_GRACE_PERIOD) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("payment-on-hold").assertDoesNotExist()
        assertFalse(viewModel.state.value.subscriptionOnHold)

        composeRule.runOnIdle { repository.emitSubscriptionState(SubscriptionState.ON_HOLD) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("payment-on-hold").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Payment needs attention").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Update payment in Google Play").performScrollTo().assertIsDisplayed()

        composeRule.runOnIdle { repository.emitSubscriptionState(SubscriptionState.ACTIVE) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("payment-on-hold").assertDoesNotExist()
    }

    @Test
    fun `grace paused canceled expired and pending states hide the on-hold card`() {
        val repository = FakeSubscriptionRepository()
        launch(repository)
        composeRule.runOnIdle { repository.emitSubscriptionState(SubscriptionState.ON_HOLD) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("payment-on-hold").assertExists()

        val hidden = listOf(
            SubscriptionState.IN_GRACE_PERIOD,
            SubscriptionState.PAUSED,
            SubscriptionState.CANCELED,
            SubscriptionState.EXPIRED,
            SubscriptionState.PENDING,
            SubscriptionState.UNKNOWN,
        )
        for (state in hidden) {
            composeRule.runOnIdle { repository.emitSubscriptionState(state) }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("payment-on-hold").assertDoesNotExist()
        }
    }

    @Test
    fun `updating a held payment opens the Play subscriptions page`() {
        val repository = FakeSubscriptionRepository()
        launch(repository)
        composeRule.runOnIdle { repository.emitSubscriptionState(SubscriptionState.ON_HOLD) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Update payment in Google Play").performScrollTo().performClick()
        composeRule.waitForIdle()

        val intent = shadowOf(composeRule.activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(
            "https://play.google.com/store/account/subscriptions?package=com.jaimesorrow.wiccan",
            intent.data.toString(),
        )
    }

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
