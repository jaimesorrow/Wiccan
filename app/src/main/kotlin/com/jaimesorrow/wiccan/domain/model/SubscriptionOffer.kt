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
    /** Play-owned lapsed-subscriber promotion, accepted only when tagged `winback`. */
    val isWinBack: Boolean = false,
    /** First-period promotional price shown by Play for a validated win-back offer. */
    val winBackFormattedPrice: String? = null,
    /** Play billing period of the win-back intro phase, e.g. `P1M` or `P1Y`. */
    val winBackBillingPeriod: String? = null,
)

/** Yearly plan change offer shown only when switching from an active monthly subscription. */
data class YearlySwitchOffer(
    val offerToken: String,
    val yearlyFormattedPrice: String,
    /** First-month promotional price from Play, when the `switch-yearly` offer is present. */
    val introFormattedPrice: String? = null,
    val priceAmountMicros: Long = 0L,
    val priceCurrencyCode: String = "",
)

/** Server-mirrored Play state; UNKNOWN is the safe fallback for absent/legacy profiles. */
enum class SubscriptionState {
    ACTIVE,
    IN_GRACE_PERIOD,
    ON_HOLD,
    PAUSED,
    CANCELED,
    EXPIRED,
    PENDING,
    UNKNOWN,
    ;

    companion object {
        fun fromWire(value: String?): SubscriptionState = when (value) {
            "active" -> ACTIVE
            "in_grace_period" -> IN_GRACE_PERIOD
            "on_hold" -> ON_HOLD
            "paused" -> PAUSED
            "canceled" -> CANCELED
            "expired" -> EXPIRED
            "pending" -> PENDING
            else -> UNKNOWN
        }
    }
}
