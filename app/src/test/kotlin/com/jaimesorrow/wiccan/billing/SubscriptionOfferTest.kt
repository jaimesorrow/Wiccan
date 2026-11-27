package com.jaimesorrow.wiccan.billing

import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.jaimesorrow.wiccan.domain.model.SubscriptionPlan
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
        assertEquals(7, selected.freeTrialDays)
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
    fun `a one-week Play billing period is the same seven-day trial`() {
        assertEquals(7, product(offer("trial", trial(period = "P1W"), renewal())).subscriptionOffer()!!.freeTrialDays)
        assertEquals(7, product(offer("trial", trial(nonRecurring = true), renewal())).subscriptionOffer()!!.freeTrialDays)
    }

    @Test(expected = PlayBillingException::class)
    fun `checkout cannot replace a displayed trial with an immediate monthly charge`() {
        val displayed = product(offer("trial", trial(), renewal())).subscriptionOffer()!!
        val nowIneligible = product(offer(null, renewal()))
        nowIneligible.requireUnchangedOffer(displayed)
    }

    @Test
    fun `yearly periods on the monthly product and unsupported promotional phases fail closed`() {
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
    fun `the yearly product never accepts monthly terms`() {
        val product = product(offer(null, renewal()))
        every { product.productId } returns PREMIUM_YEARLY_PRODUCT_ID
        assertNull(product.subscriptionOffer())
    }

    @Test
    fun `yearly plan at US 39_99 with a seven-day trial is offered as the yearly plan`() {
        val product = product(
            offer(null, renewal(period = "P1Y", price = "$39.99", micros = 39_990_000)),
            offer("trial", trial(), renewal(period = "P1Y", price = "$39.99", micros = 39_990_000)),
        )
        every { product.productId } returns PREMIUM_YEARLY_PRODUCT_ID
        val selected = product.subscriptionOffer()!!
        assertEquals(SubscriptionPlan.YEARLY, selected.plan)
        assertTrue(selected.hasFreeTrial)
        assertEquals(7, selected.freeTrialDays)
        assertEquals("$39.99", selected.formattedPrice)
        assertEquals(39_990_000L, selected.priceAmountMicros)
        assertEquals("USD", selected.priceCurrencyCode)
    }

    @Test
    fun `yearly plan accepts a fourteen-day free trial from Play`() {
        val product = product(
            offer(null, renewal(period = "P1Y", price = "$39.99", micros = 39_990_000)),
            offer("free-trial-14d", trial(period = "P14D"), renewal(period = "P1Y", price = "$39.99", micros = 39_990_000)),
        )
        every { product.productId } returns PREMIUM_YEARLY_PRODUCT_ID
        val selected = product.subscriptionOffer()!!
        assertEquals(SubscriptionPlan.YEARLY, selected.plan)
        assertTrue(selected.hasFreeTrial)
        assertEquals(14, selected.freeTrialDays)
        assertEquals("free-trial-14d-token", selected.offerToken)
        assertEquals("$39.99", selected.formattedPrice)
    }

    @Test
    fun `a two-week Play billing period is the same fourteen-day yearly trial`() {
        val product = product(
            offer(
                "free-trial-14d",
                trial(period = "P2W", nonRecurring = true),
                renewal(period = "P1Y", price = "$39.99", micros = 39_990_000),
            ),
        )
        every { product.productId } returns PREMIUM_YEARLY_PRODUCT_ID
        assertEquals(14, product.subscriptionOffer()!!.freeTrialDays)
    }

    @Test
    fun `a Play Console price change is shown and sold at Play's price instead of hiding the plan`() {
        val yearly = product(offer(null, renewal(period = "P1Y", price = "$49.99", micros = 49_990_000)))
        every { yearly.productId } returns PREMIUM_YEARLY_PRODUCT_ID
        val yearlyOffer = yearly.subscriptionOffer()!!
        assertEquals("$49.99", yearlyOffer.formattedPrice)
        assertEquals(49_990_000L, yearlyOffer.priceAmountMicros)

        val monthlyOffer = product(offer(null, renewal(price = "$4.99", micros = 4_990_000))).subscriptionOffer()!!
        assertEquals("$4.99", monthlyOffer.formattedPrice)
    }

    @Test
    fun `a price change between display and checkout still has to be refreshed first`() {
        val displayed = product(offer(null, renewal())).subscriptionOffer()!!
        val repriced = product(offer(null, renewal(price = "$6.99", micros = 6_990_000)))
        assertTrue(runCatching { repriced.requireUnchangedOffer(displayed) }.exceptionOrNull() is PlayBillingException)
    }

    @Test
    fun `Terms base prices are the single documented reference`() {
        assertEquals("$5.99", TermsBasePrices.usdText(SubscriptionPlan.MONTHLY))
        assertEquals("$39.99", TermsBasePrices.usdText(SubscriptionPlan.YEARLY))
    }

    @Test
    fun `monthly offer is labeled monthly`() {
        assertEquals(SubscriptionPlan.MONTHLY, product(offer(null, renewal())).subscriptionOffer()!!.plan)
    }

    @Test
    fun `tagged winback discount is accepted and untagged discounts fail closed`() {
        val discount = discounted()
        val tagged = product(offer("returning", discount, renewal(), tags = listOf("winback"))).subscriptionOffer()!!
        assertTrue(tagged.isWinBack)
        assertEquals("$2.99", tagged.winBackFormattedPrice)

        assertNull(product(offer("returning", discount, renewal())).subscriptionOffer())
        assertNull(product(offer("returning", discount, renewal(), tags = listOf("other"))).subscriptionOffer())
    }

    @Test
    fun `a tagged base plan is still offered as the regular plan`() {
        val selected = product(offer(null, renewal(), tags = listOf("console"))).subscriptionOffer()!!
        assertFalse(selected.isWinBack)
        assertFalse(selected.hasFreeTrial)
        assertNull(selected.winBackFormattedPrice)
        assertEquals("base-token", selected.offerToken)
        assertEquals("$5.99", selected.formattedPrice)
    }

    @Test
    fun `winback is selected from its own tags even when the base plan is tagged`() {
        val selected = product(
            offer(null, renewal(), tags = listOf("console")),
            offer("returning", discounted(), renewal(), tags = listOf("winback")),
        ).subscriptionOffer()!!
        assertTrue(selected.isWinBack)
        assertEquals("$2.99", selected.winBackFormattedPrice)
        assertEquals("returning-token", selected.offerToken)
        assertEquals("$5.99", selected.formattedPrice)
    }

    @Test
    fun `a base plan tagged winback is never treated as a winback offer`() {
        val selected = product(offer(null, renewal(), tags = listOf("winback"))).subscriptionOffer()!!
        assertFalse(selected.isWinBack)
        assertNull(selected.winBackFormattedPrice)
        assertEquals("base-token", selected.offerToken)
    }

    @Test
    fun `a seven-day trial is preferred over a winback offer`() {
        val selected = product(
            offer(null, renewal(), tags = listOf("console")),
            offer("trial", trial(), renewal()),
            offer("returning", discounted(), renewal(), tags = listOf("winback")),
        ).subscriptionOffer()!!
        assertTrue(selected.hasFreeTrial)
        assertFalse(selected.isWinBack)
        assertNull(selected.winBackFormattedPrice)
        assertEquals("trial-token", selected.offerToken)
    }

    @Test
    fun `invalid winback phases fail closed and do not hide the regular plan`() {
        fun invalid(discount: ProductDetails.PricingPhase, tags: List<String> = listOf("winback")) =
            product(offer("returning", discount, renewal(), tags = tags)).subscriptionOffer()

        assertNull(invalid(discounted(micros = 5_990_000)))
        assertNull(invalid(discounted(period = "P7D")))
        assertNull(invalid(discounted(cycles = 2)))
        assertNull(invalid(discounted(price = "$0.00", micros = 0)))
        assertNull(invalid(discounted(currency = "GBP")))
        assertNull(product(offer("returning", discounted(), discounted(), renewal(), tags = listOf("winback"))).subscriptionOffer())
        assertNull(invalid(discounted(), tags = listOf("Winback")))

        val fallback = product(
            offer(null, renewal()),
            offer("returning", discounted(period = "P7D"), renewal(), tags = listOf("winback")),
        ).subscriptionOffer()!!
        assertFalse(fallback.isWinBack)
        assertEquals("base-token", fallback.offerToken)
        assertEquals(null, fallback.winBackFormattedPrice)
    }

    @Test
    fun `a valid winback offer is preferred over the regular plan but not over a trial`() {
        val winbackAndBase = product(
            offer(null, renewal()),
            offer("returning", discounted(), renewal(), tags = listOf("promo", "winback")),
        ).subscriptionOffer()!!
        assertTrue(winbackAndBase.isWinBack)
        assertEquals("$2.99", winbackAndBase.winBackFormattedPrice)
        assertEquals("$5.99", winbackAndBase.formattedPrice)
        assertEquals("returning-token", winbackAndBase.offerToken)
        assertFalse(winbackAndBase.hasFreeTrial)

        val trialWins = product(
            offer("returning", discounted(), renewal(), tags = listOf("winback")),
            offer("trial", trial(), renewal()),
        ).subscriptionOffer()!!
        assertTrue(trialWins.hasFreeTrial)
        assertFalse(trialWins.isWinBack)
        assertEquals("trial-token", trialWins.offerToken)
        assertEquals(null, trialWins.winBackFormattedPrice)
    }

    @Test
    fun `a tagged yearly winback discount is accepted`() {
        val product = product(
            offer(
                "returning",
                discounted(period = "P1Y", price = "$19.99", micros = 19_990_000),
                renewal(period = "P1Y", price = "$39.99", micros = 39_990_000),
                tags = listOf("winback"),
            ),
        )
        every { product.productId } returns PREMIUM_YEARLY_PRODUCT_ID
        val selected = product.subscriptionOffer()!!
        assertEquals(SubscriptionPlan.YEARLY, selected.plan)
        assertTrue(selected.isWinBack)
        assertEquals("$19.99", selected.winBackFormattedPrice)
        assertEquals("$39.99", selected.formattedPrice)
    }

    @Test
    fun `a Play single-payment monthly winback is accepted`() {
        val selected = product(
            offer("winback-1mo-099", singlePayment(), renewal(), tags = listOf("winback")),
        ).subscriptionOffer()!!
        assertTrue(selected.isWinBack)
        assertEquals("$0.99", selected.winBackFormattedPrice)
        assertEquals("P1M", selected.winBackBillingPeriod)
        assertEquals("$5.99", selected.formattedPrice)
        assertEquals("winback-1mo-099-token", selected.offerToken)
        assertFalse(selected.hasFreeTrial)
    }

    @Test
    fun `a Play single-payment yearly winback uses a one-month intro on the yearly plan`() {
        val product = product(
            offer(null, renewal(period = "P1Y", price = "$39.99", micros = 39_990_000)),
            offer(
                "winback-1mo-099",
                singlePayment(),
                renewal(period = "P1Y", price = "$39.99", micros = 39_990_000),
                tags = listOf("winback"),
            ),
        )
        every { product.productId } returns PREMIUM_YEARLY_PRODUCT_ID
        val selected = product.subscriptionOffer()!!
        assertEquals(SubscriptionPlan.YEARLY, selected.plan)
        assertTrue(selected.isWinBack)
        assertEquals("$0.99", selected.winBackFormattedPrice)
        assertEquals("P1M", selected.winBackBillingPeriod)
        assertEquals("$39.99", selected.formattedPrice)
        assertFalse(selected.hasFreeTrial)
    }

    @Test
    fun `switch-yearly single-payment offer is used for a plan change and hidden from the paywall`() {
        val product = product(
            offer(null, renewal(period = "P1Y", price = "$39.99", micros = 39_990_000)),
            offer("trial", trial(), renewal(period = "P1Y", price = "$39.99", micros = 39_990_000)),
            offer(
                "switch-yearly-1mo-099",
                singlePayment(),
                renewal(period = "P1Y", price = "$39.99", micros = 39_990_000),
                tags = listOf("switch-yearly"),
            ),
        )
        every { product.productId } returns PREMIUM_YEARLY_PRODUCT_ID

        val paywall = product.subscriptionOffer()!!
        assertTrue(paywall.hasFreeTrial)
        assertFalse(paywall.isWinBack)
        assertEquals("trial-token", paywall.offerToken)

        val switch = product.yearlySwitchOffer()!!
        assertEquals("switch-yearly-1mo-099-token", switch.offerToken)
        assertEquals("$0.99", switch.introFormattedPrice)
        assertEquals("$39.99", switch.yearlyFormattedPrice)
    }

    @Test
    fun `yearly switch falls back to the base plan when the tagged offer is missing or invalid`() {
        val missing = product(offer(null, renewal(period = "P1Y", price = "$39.99", micros = 39_990_000)))
        every { missing.productId } returns PREMIUM_YEARLY_PRODUCT_ID
        val fallback = missing.yearlySwitchOffer()!!
        assertEquals("base-token", fallback.offerToken)
        assertEquals(null, fallback.introFormattedPrice)
        assertEquals("$39.99", fallback.yearlyFormattedPrice)

        val invalidTagged = product(
            offer(null, renewal(period = "P1Y", price = "$39.99", micros = 39_990_000)),
            offer(
                "switch-yearly-1mo-099",
                trial(),
                renewal(period = "P1Y", price = "$39.99", micros = 39_990_000),
                tags = listOf("switch-yearly"),
            ),
        )
        every { invalidTagged.productId } returns PREMIUM_YEARLY_PRODUCT_ID
        val afterInvalid = invalidTagged.yearlySwitchOffer()!!
        assertEquals("base-token", afterInvalid.offerToken)
        assertEquals(null, afterInvalid.introFormattedPrice)
    }

    @Test
    fun `a base plan tagged switch-yearly is not treated as the promo offer`() {
        val product = product(
            offer(null, renewal(period = "P1Y", price = "$39.99", micros = 39_990_000), tags = listOf("switch-yearly")),
        )
        every { product.productId } returns PREMIUM_YEARLY_PRODUCT_ID
        val switch = product.yearlySwitchOffer()!!
        assertEquals("base-token", switch.offerToken)
        assertEquals(null, switch.introFormattedPrice)
    }

    @Test
    fun `a tagged switch offer with a first-year intro is not selected`() {
        val product = product(
            offer(null, renewal(period = "P1Y", price = "$39.99", micros = 39_990_000)),
            offer(
                "switch-yearly-year",
                discounted(period = "P1Y", price = "$19.99", micros = 19_990_000),
                renewal(period = "P1Y", price = "$39.99", micros = 39_990_000),
                tags = listOf("switch-yearly"),
            ),
        )
        every { product.productId } returns PREMIUM_YEARLY_PRODUCT_ID
        val switch = product.yearlySwitchOffer()!!
        assertEquals("base-token", switch.offerToken)
        assertEquals(null, switch.introFormattedPrice)
    }

    @Test
    fun `checkout cannot replace a displayed switch offer with different yearly terms`() {
        val displayed = product(
            offer(
                "switch-yearly-1mo-099",
                singlePayment(),
                renewal(period = "P1Y", price = "$39.99", micros = 39_990_000),
                tags = listOf("switch-yearly"),
            ),
        )
        every { displayed.productId } returns PREMIUM_YEARLY_PRODUCT_ID
        val offer = displayed.yearlySwitchOffer()!!
        val nowBaseOnly = product(offer(null, renewal(period = "P1Y", price = "$39.99", micros = 39_990_000)))
        every { nowBaseOnly.productId } returns PREMIUM_YEARLY_PRODUCT_ID
        assertTrue(runCatching { nowBaseOnly.requireUnchangedSwitchOffer(offer) }.exceptionOrNull() is PlayBillingException)
    }

    @Test
    fun `monthly to yearly replacement uses time proration`() {
        // Billing Library 9 validates tokens with TextUtils, which is only
        // reliable under Robolectric or on a device. The mode itself is the
        // part this test can pin down here.
        assertEquals(
            BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION,
            YEARLY_SWITCH_REPLACEMENT_MODE,
        )
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

    private fun offer(
        id: String?,
        vararg phaseList: ProductDetails.PricingPhase,
        tags: List<String> = emptyList(),
    ): ProductDetails.SubscriptionOfferDetails {
        val phases = mockk<ProductDetails.PricingPhases> { every { pricingPhaseList } returns phaseList.toList() }
        return mockk {
            every { offerId } returns id
            every { offerToken } returns "${id ?: "base"}-token"
            every { offerTags } returns tags
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

    private fun trial(period: String = "P7D", nonRecurring: Boolean = false): ProductDetails.PricingPhase = mockk {
        every { recurrenceMode } returns if (nonRecurring) {
            ProductDetails.RecurrenceMode.NON_RECURRING
        } else {
            ProductDetails.RecurrenceMode.FINITE_RECURRING
        }
        every { billingPeriod } returns period
        every { priceAmountMicros } returns 0L
        every { billingCycleCount } returns if (nonRecurring) 0 else 1
    }

    private fun singlePayment(
        period: String = "P1M",
        currency: String = "USD",
        price: String = "$0.99",
        micros: Long = 990_000,
        cycles: Int = 0,
    ): ProductDetails.PricingPhase = mockk {
        every { recurrenceMode } returns ProductDetails.RecurrenceMode.NON_RECURRING
        every { billingPeriod } returns period
        every { priceCurrencyCode } returns currency
        every { formattedPrice } returns price
        every { priceAmountMicros } returns micros
        every { billingCycleCount } returns cycles
    }

    private fun discounted(
        period: String = "P1M",
        currency: String = "USD",
        price: String = "$2.99",
        micros: Long = 2_990_000,
        cycles: Int = 1,
    ): ProductDetails.PricingPhase = mockk {
        every { recurrenceMode } returns ProductDetails.RecurrenceMode.FINITE_RECURRING
        every { billingPeriod } returns period
        every { priceCurrencyCode } returns currency
        every { formattedPrice } returns price
        every { priceAmountMicros } returns micros
        every { billingCycleCount } returns cycles
    }
}
