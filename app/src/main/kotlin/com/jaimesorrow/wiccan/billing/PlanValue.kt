package com.jaimesorrow.wiccan.billing

import com.jaimesorrow.wiccan.domain.model.SubscriptionOffer
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * The yearly plan's price per month, formatted in Play's own currency
 * (e.g. "$3.33" for US $39.99), or null when it can't be computed honestly.
 * Rounded down so the paywall never shows a per-month figure above the truth.
 */
fun yearlyMonthlyEquivalent(yearly: SubscriptionOffer, locale: Locale = Locale.getDefault()): String? {
    if (yearly.priceAmountMicros <= 0 || yearly.priceCurrencyCode.isBlank()) return null
    val currency = runCatching { Currency.getInstance(yearly.priceCurrencyCode) }.getOrNull() ?: return null
    val digits = currency.defaultFractionDigits.coerceAtLeast(0)
    val perMonth = BigDecimal.valueOf(yearly.priceAmountMicros)
        .divide(BigDecimal.valueOf(12_000_000L), digits, RoundingMode.DOWN)
    return NumberFormat.getCurrencyInstance(locale).apply {
        this.currency = currency
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }.format(perMonth)
}

/**
 * Whole-percent saving of the yearly plan against twelve monthly payments
 * (44 for $39.99 vs $5.99), rounded down. Null unless both prices come from
 * Play in the same currency and the yearly plan really is cheaper.
 */
fun yearlySavingsPercent(yearly: SubscriptionOffer, monthly: SubscriptionOffer): Int? {
    if (yearly.priceCurrencyCode.isBlank() || yearly.priceCurrencyCode != monthly.priceCurrencyCode) return null
    val twelveMonths = monthly.priceAmountMicros * 12
    if (yearly.priceAmountMicros <= 0 || twelveMonths <= yearly.priceAmountMicros) return null
    val percent = ((twelveMonths - yearly.priceAmountMicros) * 100 / twelveMonths).toInt()
    return percent.takeIf { it > 0 }
}
