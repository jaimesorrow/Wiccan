package com.jaimesorrow.wiccan.billing

import com.android.billingclient.api.Purchase
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.SubscriptionState
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun `switch token is the monthly purchase bound to the signed-in account`() {
        val expected = obfuscatedAccountId("uid-123")
        val token = monthlySwitchPurchaseToken(
            listOf(
                purchase("other-monthly", accountId = obfuscatedAccountId("other-user")),
                purchase("mine-monthly", accountId = expected),
            ),
            expectedAccountId = expected,
            serverPurchaseToken = null,
        )
        assertEquals("mine-monthly", token)
    }

    @Test
    fun `switch token is not the first monthly subscription on the Google account`() {
        val expected = obfuscatedAccountId("uid-123")
        assertNull(
            monthlySwitchPurchaseToken(
                listOf(purchase("other-monthly", accountId = obfuscatedAccountId("other-user"))),
                expectedAccountId = expected,
                serverPurchaseToken = null,
            ),
        )
        assertNull(
            monthlySwitchPurchaseToken(
                listOf(purchase("unbound-monthly")),
                expectedAccountId = expected,
                serverPurchaseToken = "server-token",
            ),
        )
    }

    @Test
    fun `a legacy purchase without an account hash may use the server-verified token`() {
        val expected = obfuscatedAccountId("uid-123")
        assertEquals(
            "server-token",
            monthlySwitchPurchaseToken(
                listOf(
                    purchase("other-monthly", accountId = obfuscatedAccountId("other-user")),
                    purchase("server-token"),
                ),
                expectedAccountId = expected,
                serverPurchaseToken = "server-token",
            ),
        )
    }

    private fun purchase(token: String, accountId: String? = null): Purchase = mockk {
        every { purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { products } returns listOf(PREMIUM_MONTHLY_PRODUCT_ID)
        every { purchaseToken } returns token
        every { accountIdentifiers } returns if (accountId == null) {
            null
        } else {
            mockk { every { obfuscatedAccountId } returns accountId }
        }
    }
}
