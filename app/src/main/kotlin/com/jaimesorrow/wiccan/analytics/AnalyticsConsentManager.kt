package com.jaimesorrow.wiccan.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.ConsentStatus
import com.google.firebase.analytics.FirebaseAnalytics.ConsentType
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jaimesorrow.wiccan.data.local.AnalyticsConsentPreferences
import com.wiccan.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Applies the user's analytics/crash-report choice to the Firebase SDKs.
 * The manifest starts both SDKs with collection off and every consent-mode
 * signal denied, so nothing is collected before [applyStoredConsent] runs
 * or before the user opts in. Ad-related consent is always denied: Wiccan
 * shows no ads, so the advertising ID is never used for advertising.
 *
 * Every read/persist/apply sequence runs under one lock, so a slow startup
 * read can never re-enable collection after the user has withdrawn.
 */
@Singleton
class AnalyticsConsentManager @Inject constructor(
    private val preferences: AnalyticsConsentPreferences,
    private val analytics: FirebaseAnalytics,
    private val crashlytics: FirebaseCrashlytics,
) {
    private val lock = Mutex()

    val consent: Flow<Boolean> = preferences.analyticsConsent

    /** Re-applies the stored choice at process start; an unreadable store means no consent. */
    suspend fun applyStoredConsent() = lock.withLock {
        apply(readStored() ?: false)
    }

    /**
     * Records and applies a choice; returns false if it could not be saved.
     * Withdrawal takes effect before (and regardless of) saving. Opt-in
     * enables collection only once it is saved, after discarding any crash
     * reports buffered on the device before consent.
     */
    suspend fun setConsent(granted: Boolean): Boolean = lock.withLock {
        val previously = readStored() ?: false
        if (!granted) {
            apply(false)
            if (previously) analytics.resetAnalyticsData()
        }
        val saved = persist(granted)
        if (granted && saved) {
            if (!previously) crashlytics.deleteUnsentReports()
            apply(true)
        }
        saved
    }

    private suspend fun readStored(): Boolean? = try {
        preferences.analyticsConsent.first()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private suspend fun persist(granted: Boolean): Boolean = try {
        preferences.setAnalyticsConsent(granted)
        true
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
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
