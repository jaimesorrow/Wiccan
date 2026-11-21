package com.jaimesorrow.wiccan.billing

import com.jaimesorrow.wiccan.domain.model.SubscriptionOffer
import com.jaimesorrow.wiccan.domain.model.SubscriptionPlan
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlanValueTest {
    private val monthly = SubscriptionOffer("$5.99", true, "m", SubscriptionPlan.MONTHLY, 5_990_000L, "USD")
    private val yearly = SubscriptionOffer("$39.99", true, "y", SubscriptionPlan.YEARLY, 39_990_000L, "USD")

    @Test
    fun `US 39_99 a year is 3_33 a month rounded down`() {
        assertEquals("$3.33", yearlyMonthlyEquivalent(yearly, Locale.US))
    }

    @Test
    fun `US 39_99 a year saves 44 percent against twelve monthly payments`() {
        assertEquals(44, yearlySavingsPercent(yearly, monthly))
    }

    @Test
    fun `no saving is claimed across currencies or when yearly is not cheaper`() {
        assertNull(yearlySavingsPercent(yearly.copy(priceCurrencyCode = "EUR"), monthly))
        assertNull(yearlySavingsPercent(yearly.copy(priceAmountMicros = 71_880_000L), monthly))
    }

    @Test
    fun `zero-decimal currencies and unknown prices are handled honestly`() {
        val yen = yearly.copy(priceAmountMicros = 6_000_000_000L, priceCurrencyCode = "JPY")
        assertEquals("¥500", yearlyMonthlyEquivalent(yen, Locale.JAPAN)!!.replace('￥', '¥'))
        assertNull(yearlyMonthlyEquivalent(yearly.copy(priceAmountMicros = 0L)))
        assertNull(yearlyMonthlyEquivalent(yearly.copy(priceCurrencyCode = "")))
    }
}
