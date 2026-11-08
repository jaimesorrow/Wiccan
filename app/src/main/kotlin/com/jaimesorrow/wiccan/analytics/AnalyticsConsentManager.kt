package com.jaimesorrow.wiccan.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.ConsentStatus
import com.google.firebase.analytics.FirebaseAnalytics.ConsentType
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jaimesorrow.wiccan.data.local.AnalyticsConsentPreferences
import com.wiccan.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Applies the user's analytics/crash-report choice to the Firebase SDKs.
 * The manifest starts both SDKs with collection off and every consent-mode
 * signal denied, so nothing is collected before [applyStoredConsent] runs
 * or before the user opts in. Ad-related consent is always denied: Wiccan
 * shows no ads, so the advertising ID is never used for advertising.
 */
@Singleton
class AnalyticsConsentManager @Inject constructor(
    private val preferences: AnalyticsConsentPreferences,
    private val analytics: FirebaseAnalytics,
    private val crashlytics: FirebaseCrashlytics,
) {
    val consent: Flow<Boolean> = preferences.analyticsConsent

    /** Re-applies the stored choice at process start. */
    suspend fun applyStoredConsent() = apply(preferences.analyticsConsent.first())

    suspend fun setConsent(granted: Boolean) {
        val previously = preferences.analyticsConsent.first()
        preferences.setAnalyticsConsent(granted)
        apply(granted)
        if (previously && !granted) {
            // Withdrawal: drop the analytics instance ID and anything not yet sent.
            analytics.resetAnalyticsData()
        }
    }

    private fun apply(granted: Boolean) {
        val collect = granted && !BuildConfig.USE_FIREBASE_EMULATORS
        val analyticsStorage = if (collect) ConsentStatus.GRANTED else ConsentStatus.DENIED
        analytics.setConsent(
            mapOf(
                ConsentType.ANALYTICS_STORAGE to analyticsStorage,
                ConsentType.AD_STORAGE to ConsentStatus.DENIED,
                ConsentType.AD_USER_DATA to ConsentStatus.DENIED,
                ConsentType.AD_PERSONALIZATION to ConsentStatus.DENIED,
            ),
        )
        analytics.setAnalyticsCollectionEnabled(collect)
        crashlytics.setCrashlyticsCollectionEnabled(collect)
        if (!collect) crashlytics.deleteUnsentReports()
    }
}
