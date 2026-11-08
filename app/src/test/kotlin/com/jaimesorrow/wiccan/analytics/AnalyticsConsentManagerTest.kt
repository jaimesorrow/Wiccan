package com.jaimesorrow.wiccan.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jaimesorrow.wiccan.data.local.AnalyticsConsentPreferences
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsConsentManagerTest {
    private val stored = MutableStateFlow(false)
    private val preferences: AnalyticsConsentPreferences = mockk {
        every { analyticsConsent } returns stored
        coEvery { setAnalyticsConsent(any()) } answers { stored.value = firstArg() }
    }
    private val analytics: FirebaseAnalytics = mockk(relaxed = true)
    private val crashlytics: FirebaseCrashlytics = mockk(relaxed = true)
    private val manager = AnalyticsConsentManager(preferences, analytics, crashlytics)

    @Test
    fun `startup without consent keeps both SDKs off`() = runTest {
        manager.applyStoredConsent()
        verify { analytics.setAnalyticsCollectionEnabled(false) }
        verify { crashlytics.setCrashlyticsCollectionEnabled(false) }
    }

    @Test
    fun `opting in drops pre-consent crash buffers before enabling collection`() = runTest {
        assertTrue(manager.setConsent(true))
        verifyOrder {
            crashlytics.deleteUnsentReports()
            crashlytics.setCrashlyticsCollectionEnabled(true)
        }
        verify { analytics.setAnalyticsCollectionEnabled(true) }
    }

    @Test
    fun `a failed opt-in save leaves collection off`() = runTest {
        coEvery { preferences.setAnalyticsConsent(true) } throws IOException("disk full")
        assertFalse(manager.setConsent(true))
        verify(exactly = 0) { analytics.setAnalyticsCollectionEnabled(true) }
        verify(exactly = 0) { crashlytics.setCrashlyticsCollectionEnabled(true) }
    }

    @Test
    fun `withdrawal turns collection off even when saving fails, and resets analytics data`() = runTest {
        stored.value = true
        coEvery { preferences.setAnalyticsConsent(false) } throws IOException("disk full")
        assertFalse(manager.setConsent(false))
        verify { analytics.setAnalyticsCollectionEnabled(false) }
        verify { crashlytics.setCrashlyticsCollectionEnabled(false) }
        verify { analytics.resetAnalyticsData() }
    }

    @Test
    fun `an unreadable store at startup means no consent`() = runTest {
        every { preferences.analyticsConsent } returns kotlinx.coroutines.flow.flow { throw IOException("corrupt") }
        val unreadable = AnalyticsConsentManager(preferences, analytics, crashlytics)
        unreadable.applyStoredConsent()
        verify { analytics.setAnalyticsCollectionEnabled(false) }
    }
}
