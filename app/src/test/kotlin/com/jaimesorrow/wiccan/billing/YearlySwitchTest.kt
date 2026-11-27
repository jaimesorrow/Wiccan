package com.jaimesorrow.wiccan.billing

import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.SubscriptionState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class YearlySwitchTest {
    @Test
    fun `only an active monthly premium subscriber can switch when an offer is available`() {
        assertTrue(
            canShowYearlySwitch(
                ContentTier.PREMIUM,
                SubscriptionState.ACTIVE,
                PREMIUM_MONTHLY_PRODUCT_ID,
                switchOfferAvailable = true,
            ),
        )
    }

    @Test
    fun `grace hold yearly free and missing offers hide the switch`() {
        val hiddenStates = listOf(
            SubscriptionState.IN_GRACE_PERIOD,
            SubscriptionState.ON_HOLD,
            SubscriptionState.PAUSED,
            SubscriptionState.CANCELED,
            SubscriptionState.EXPIRED,
            SubscriptionState.PENDING,
            SubscriptionState.UNKNOWN,
        )
        hiddenStates.forEach { state ->
            assertFalse(
                canShowYearlySwitch(
                    ContentTier.PREMIUM,
                    state,
                    PREMIUM_MONTHLY_PRODUCT_ID,
                    switchOfferAvailable = true,
                ),
            )
        }
        assertFalse(
            canShowYearlySwitch(
                ContentTier.FREE,
                SubscriptionState.ACTIVE,
                PREMIUM_MONTHLY_PRODUCT_ID,
                switchOfferAvailable = true,
            ),
        )
        assertFalse(
            canShowYearlySwitch(
                ContentTier.PREMIUM,
                SubscriptionState.ACTIVE,
                PREMIUM_YEARLY_PRODUCT_ID,
                switchOfferAvailable = true,
            ),
        )
        assertFalse(
            canShowYearlySwitch(
                ContentTier.PREMIUM,
                SubscriptionState.ACTIVE,
                null,
                switchOfferAvailable = true,
            ),
        )
        assertFalse(
            canShowYearlySwitch(
                ContentTier.PREMIUM,
                SubscriptionState.ACTIVE,
                PREMIUM_MONTHLY_PRODUCT_ID,
                switchOfferAvailable = false,
            ),
        )
    }

    @Test
    fun `replacement params accept a monthly purchase token`() {
        yearlySwitchUpdateParams("monthly-token")
    }
}
