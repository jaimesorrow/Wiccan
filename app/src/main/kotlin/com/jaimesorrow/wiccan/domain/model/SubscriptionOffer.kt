package com.jaimesorrow.wiccan.domain.model

/** An eligible Google Play offer; the same terms must be used at checkout. */
data class SubscriptionOffer(
    val formattedPrice: String,
    val hasFreeTrial: Boolean,
    val offerToken: String,
)
