package com.wiccan.app.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.wiccan.app.domain.model.ContentTier
import com.wiccan.app.domain.model.ContentType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around the exactly six Analytics events this app logs — no
 * additional instrumentation lives here or anywhere else, per spec.
 */
@Singleton
class AnalyticsLogger @Inject constructor(
    private val analytics: FirebaseAnalytics,
) {
    /** First successful sign-in — a brand-new account, not a returning user. */
    fun logSignUpCompleted() {
        analytics.logEvent("sign_up_completed", null)
    }

    /** Today displays content. */
    fun logDailyContentViewed(type: ContentType, tier: ContentTier) {
        val params = Bundle().apply {
            putString("type", type.name)
            putString("tier", tier.name)
        }
        analytics.logEvent("daily_content_viewed", params)
    }

    /** Paywall shown. */
    fun logPaywallViewed() {
        analytics.logEvent("paywall_viewed", null)
    }

    /** A purchase was verified active. */
    fun logSubscriptionStarted() {
        analytics.logEvent("subscription_started", null)
    }

    /** The scheduled lapsed-subscription re-check downgraded this user. */
    fun logSubscriptionLapsed() {
        analytics.logEvent("subscription_lapsed", null)
    }

    /** Account deletion completed. */
    fun logAccountDeleted() {
        analytics.logEvent("account_deleted", null)
    }
}
