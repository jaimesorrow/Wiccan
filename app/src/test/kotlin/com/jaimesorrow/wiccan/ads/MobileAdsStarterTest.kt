package com.jaimesorrow.wiccan.ads

import com.google.android.gms.ads.RequestConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileAdsStarterTest {

    private class RecordingSdk(var failInitialize: Boolean = false) : MobileAdsSdk {
        val calls = mutableListOf<String>()
        var policy: AdRequestPolicy? = null
        var complete: (() -> Unit)? = null

        override fun setRequestConfiguration(policy: AdRequestPolicy) {
            calls += "setRequestConfiguration"
            this.policy = policy
        }

        override fun initialize(onComplete: () -> Unit) {
            calls += "initialize"
            if (failInitialize) throw IllegalStateException("boom")
            complete = onComplete
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `18+ request configuration is set before initialize`() {
        val sdk = RecordingSdk()
        MobileAdsStarter(sdk).start()
        assertEquals(listOf("setRequestConfiguration", "initialize"), sdk.calls)
        val policy = sdk.policy!!
        assertEquals(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE, policy.tagForChildDirectedTreatment)
        assertEquals(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE, policy.tagForUnderAgeOfConsent)
        assertEquals(RequestConfiguration.MAX_AD_CONTENT_RATING_MA, policy.maxAdContentRating)
    }

    @Test
    fun `ready only after the initialization callback`() {
        val sdk = RecordingSdk()
        val starter = MobileAdsStarter(sdk)
        starter.start()
        assertFalse("initialize called but not complete", starter.ready.value)
        sdk.complete!!.invoke()
        assertTrue(starter.ready.value)
    }

    @Test
    fun `starts once`() {
        val sdk = RecordingSdk()
        val starter = MobileAdsStarter(sdk)
        starter.start()
        starter.start()
        assertEquals(1, sdk.calls.count { it == "initialize" })
    }

    @Test
    fun `failed initialize stays not ready and can retry`() {
        val sdk = RecordingSdk(failInitialize = true)
        var failures = 0
        val starter = MobileAdsStarter(sdk) { failures++ }
        starter.start()
        assertFalse(starter.ready.value)
        assertEquals(1, failures)
        sdk.failInitialize = false
        starter.start()
        sdk.complete!!.invoke()
        assertTrue(starter.ready.value)
    }
}
