package com.jaimesorrow.wiccan.billing

import com.jaimesorrow.wiccan.domain.model.SubscriptionPlan

/**
 * The US base prices published in the Terms ("Subscriptions and billing")
 * and set in Play Console. This is the single place they are written down
 * in the app.
 *
 * They are reference values only, never a gate: the paywall always shows
 * and sells Google Play's live price from ProductDetails, so a Play Console
 * price change cannot silently break checkout or hide a plan.
 * `LegalDocumentsTest` checks that the Terms quote these numbers, so changing
 * a price here fails the build until the Terms (and the policy version) are
 * updated too. To change a price: update Play Console, this object, the
 * Terms text in LegalDocuments.kt and hosting/terms.html, and bump
 * PolicyVersions. See docs/V1_1_PLAN.md, "Price guard".
 */
object TermsBasePrices {
    const val MONTHLY_USD_MICROS = 5_990_000L
    const val YEARLY_USD_MICROS = 39_990_000L

    fun usdMicros(plan: SubscriptionPlan): Long = when (plan) {
        SubscriptionPlan.MONTHLY -> MONTHLY_USD_MICROS
        SubscriptionPlan.YEARLY -> YEARLY_USD_MICROS
    }

    /** "$5.99" style text, as the Terms write it. */
    fun usdText(plan: SubscriptionPlan): String {
        val micros = usdMicros(plan)
        val cents = micros / 10_000
        return "$" + (cents / 100) + "." + (cents % 100).toString().padStart(2, '0')
    }
}
