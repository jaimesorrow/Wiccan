package com.jaimesorrow.wiccan.domain.model

/** The two standalone Wiccan Premium plans sold in Google Play. */
enum class SubscriptionPlan { MONTHLY, YEARLY }

/** An eligible Google Play offer; the same terms must be used at checkout. */
data class SubscriptionOffer(
    val formattedPrice: String,
    val hasFreeTrial: Boolean,
    val offerToken: String,
    val plan: SubscriptionPlan = SubscriptionPlan.MONTHLY,
    /** Renewal price in micros of [priceCurrencyCode], as reported by Play (0 when unknown). */
    val priceAmountMicros: Long = 0L,
    val priceCurrencyCode: String = "",
)
