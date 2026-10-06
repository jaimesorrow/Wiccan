package com.jaimesorrow.wiccan.billing

import com.android.billingclient.api.ProductDetails
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubscriptionOfferTest {
    @Test
    fun `monthly price uses regular recurring base plan rather than introductory or annual offers`() {
        val product = product(
            PREMIUM_MONTHLY_PRODUCT_ID,
            offer("free-trial", "P1M", "$0.00"),
            offer(null, "P1Y", "$39.99"),
            offer(null, "P1M", "$4.99"),
        )

        assertEquals("$4.99", product.formattedPrice())
        assertEquals("base-P1M", product.recurringBasePlan()?.offerToken)
    }

    @Test
    fun `missing regular monthly plan does not present an introductory price as recurring`() {
        val product = product(PREMIUM_MONTHLY_PRODUCT_ID, offer("trial", "P1M", "$0.00"))

        assertNull(product.formattedPrice())
        assertNull(product.recurringBasePlan())
    }

    @Test
    fun `yearly product selects annual recurring base plan`() {
        val product = product(
            PREMIUM_YEARLY_PRODUCT_ID,
            offer(null, "P1M", "$4.99"),
            offer(null, "P1Y", "$39.99"),
        )

        assertEquals("$39.99", product.formattedPrice())
    }

    @Test
    fun `Firebase account obfuscation matches server SHA256 lowercase hex convention`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            obfuscatedAccountId("abc"),
        )
    }

    private fun product(id: String, vararg offers: ProductDetails.SubscriptionOfferDetails): ProductDetails = mockk {
        every { productId } returns id
        every { subscriptionOfferDetails } returns offers.toList()
    }

    private fun offer(id: String?, period: String, price: String): ProductDetails.SubscriptionOfferDetails {
        val phase = mockk<ProductDetails.PricingPhase> {
            every { recurrenceMode } returns ProductDetails.RecurrenceMode.INFINITE_RECURRING
            every { billingPeriod } returns period
            every { formattedPrice } returns price
        }
        val phases = mockk<ProductDetails.PricingPhases> {
            every { pricingPhaseList } returns listOf(phase)
        }
        return mockk {
            every { offerId } returns id
            every { offerToken } returns "${id ?: "base"}-$period"
            every { pricingPhases } returns phases
        }
    }
}
