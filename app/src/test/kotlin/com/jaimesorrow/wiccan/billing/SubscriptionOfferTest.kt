package com.jaimesorrow.wiccan.billing

import com.android.billingclient.api.ProductDetails
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionOfferTest {
    @Test
    fun `eligible seven-day trial is preferred and displays the monthly renewal price`() {
        val product = product(
            offer(null, renewal()),
            offer("trial", trial(), renewal()),
        )
        val selected = product.subscriptionOffer()!!
        assertTrue(selected.hasFreeTrial)
        assertEquals("$5.99", selected.formattedPrice)
        assertEquals("trial-token", selected.offerToken)
    }

    @Test
    fun `ineligible accounts receive the regular monthly plan without a trial promise`() {
        val selected = product(offer(null, renewal())).subscriptionOffer()!!
        assertFalse(selected.hasFreeTrial)
        assertEquals("base-token", selected.offerToken)
    }

    @Test
    fun `wrong US price yearly plans and unsupported promotional phases fail closed`() {
        assertNull(product(offer(null, renewal(micros = 4_990_000))).subscriptionOffer())
        assertNull(product(offer(null, renewal(period = "P1Y"))).subscriptionOffer())
        assertNull(product(offer("promo", trial(period = "P3D"), renewal())).subscriptionOffer())
        assertNull(product(offer("intro", renewal())).subscriptionOffer())
        assertNull(product(offer("trial", trial(), trial(), renewal())).subscriptionOffer())
        assertNull(product().subscriptionOffer())
    }

    @Test
    fun `localized prices are taken from Play rather than displaying a USD amount`() {
        val selected = product(offer(null, renewal(currency = "GBP", price = "£5.49", micros = 5_490_000))).subscriptionOffer()!!
        assertEquals("£5.49", selected.formattedPrice)
    }

    @Test
    fun `legacy yearly product cannot be offered at the monthly checkout`() {
        val product = product(offer(null, renewal()))
        every { product.productId } returns PREMIUM_YEARLY_PRODUCT_ID
        assertNull(product.subscriptionOffer())
    }

    @Test
    fun `Firebase account obfuscation matches server SHA256 lowercase hex convention`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            obfuscatedAccountId("abc"),
        )
    }

    private fun product(vararg offers: ProductDetails.SubscriptionOfferDetails): ProductDetails = mockk {
        every { productId } returns PREMIUM_MONTHLY_PRODUCT_ID
        every { subscriptionOfferDetails } returns offers.toList()
    }

    private fun offer(id: String?, vararg phaseList: ProductDetails.PricingPhase): ProductDetails.SubscriptionOfferDetails {
        val phases = mockk<ProductDetails.PricingPhases> { every { pricingPhaseList } returns phaseList.toList() }
        return mockk {
            every { offerId } returns id
            every { offerToken } returns "${id ?: "base"}-token"
            every { pricingPhases } returns phases
        }
    }

    private fun renewal(
        period: String = "P1M",
        currency: String = "USD",
        price: String = "$5.99",
        micros: Long = 5_990_000,
    ): ProductDetails.PricingPhase = mockk {
        every { recurrenceMode } returns ProductDetails.RecurrenceMode.INFINITE_RECURRING
        every { billingPeriod } returns period
        every { priceCurrencyCode } returns currency
        every { formattedPrice } returns price
        every { priceAmountMicros } returns micros
    }

    private fun trial(period: String = "P7D"): ProductDetails.PricingPhase = mockk {
        every { recurrenceMode } returns ProductDetails.RecurrenceMode.FINITE_RECURRING
        every { billingPeriod } returns period
        every { priceAmountMicros } returns 0
        every { billingCycleCount } returns 1
    }
}
