package com.jaimesorrow.wiccan.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.ContentType
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class AnalyticsLoggerTest {

    private val analytics: FirebaseAnalytics = mockk(relaxed = true)
    private lateinit var logger: AnalyticsLogger

    @Before
    fun setUp() {
        logger = AnalyticsLogger(analytics)
    }

    @Test
    fun `logs exactly the six named events, and no others`() {
        logger.logSignUpCompleted()
        logger.logDailyContentViewed(ContentType.SPELL, ContentTier.PREMIUM)
        logger.logPaywallViewed()
        logger.logSubscriptionStarted()
        logger.logSubscriptionLapsed()
        logger.logAccountDeleted()

        verify(exactly = 1) { analytics.logEvent("sign_up_completed", null) }
        verify(exactly = 1) { analytics.logEvent("daily_content_viewed", any()) }
        verify(exactly = 1) { analytics.logEvent("paywall_viewed", null) }
        verify(exactly = 1) { analytics.logEvent("subscription_started", null) }
        verify(exactly = 1) { analytics.logEvent("subscription_lapsed", null) }
        verify(exactly = 1) { analytics.logEvent("account_deleted", null) }
        verify(exactly = 6) { analytics.logEvent(any(), any()) }
    }
}
