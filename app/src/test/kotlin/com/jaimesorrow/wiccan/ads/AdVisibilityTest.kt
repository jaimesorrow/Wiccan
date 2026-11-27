package com.jaimesorrow.wiccan.ads

import com.jaimesorrow.wiccan.domain.model.VerifiedTier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdVisibilityTest {

    @Test
    fun `premium never shows ads`() = runTest {
        val values = adsAllowedForTier(flowOf(VerifiedTier.PREMIUM)).toList()
        assertEquals(listOf(false), values)
    }

    @Test
    fun `unverified tier never shows ads however long it holds`() = runTest {
        // Disk cache, Firestore error, the 10s verification timeout, offline and
        // signed out all arrive as UNVERIFIED.
        val tiers = MutableSharedFlow<VerifiedTier>()
        val values = mutableListOf<Boolean>()
        val job = launch { adsAllowedForTier(tiers).collect { values += it } }
        runCurrent()
        tiers.emit(VerifiedTier.UNVERIFIED)
        advanceTimeBy(FREE_TIER_SETTLE_MILLIS * 100)
        runCurrent()
        assertEquals(listOf(false), values)
        job.cancel()
    }

    @Test
    fun `server confirmed free shows ads only after the tier settles`() = runTest {
        val tiers = MutableSharedFlow<VerifiedTier>()
        val values = mutableListOf<Boolean>()
        val job = launch { adsAllowedForTier(tiers).collect { values += it } }
        runCurrent()
        tiers.emit(VerifiedTier.FREE)
        advanceTimeBy(FREE_TIER_SETTLE_MILLIS - 1)
        runCurrent()
        assertEquals(listOf(false), values)
        advanceTimeBy(2)
        runCurrent()
        assertEquals(listOf(false, true), values)
        job.cancel()
    }

    @Test
    fun `unverified then slow server premium never flashes an ad`() = runTest {
        val tiers = MutableSharedFlow<VerifiedTier>()
        val values = mutableListOf<Boolean>()
        val job = launch { adsAllowedForTier(tiers).collect { values += it } }
        runCurrent()
        tiers.emit(VerifiedTier.UNVERIFIED)
        advanceTimeBy(FREE_TIER_SETTLE_MILLIS * 4) // server slower than the settle delay
        tiers.emit(VerifiedTier.PREMIUM)
        advanceTimeBy(FREE_TIER_SETTLE_MILLIS * 4)
        runCurrent()
        assertFalse(values.contains(true))
        job.cancel()
    }

    @Test
    fun `losing server confirmation hides ads again`() = runTest {
        val tiers = MutableSharedFlow<VerifiedTier>()
        val values = mutableListOf<Boolean>()
        val job = launch { adsAllowedForTier(tiers).collect { values += it } }
        runCurrent()
        tiers.emit(VerifiedTier.FREE)
        advanceTimeBy(FREE_TIER_SETTLE_MILLIS + 1)
        runCurrent()
        assertTrue(values.last())
        tiers.emit(VerifiedTier.UNVERIFIED) // e.g. went offline and only cache answers
        runCurrent()
        assertFalse(values.last())
        job.cancel()
    }

    @Test
    fun `subscribing hides ads immediately and a confirmed lapse brings them back`() = runTest {
        val tiers = MutableSharedFlow<VerifiedTier>()
        val values = mutableListOf<Boolean>()
        val job = launch { adsAllowedForTier(tiers).collect { values += it } }
        runCurrent()
        tiers.emit(VerifiedTier.FREE)
        advanceTimeBy(FREE_TIER_SETTLE_MILLIS + 1)
        runCurrent()
        assertTrue(values.last())

        tiers.emit(VerifiedTier.PREMIUM) // monthly or yearly purchase verified
        runCurrent()
        assertFalse(values.last())

        tiers.emit(VerifiedTier.UNVERIFIED) // local expiry, not yet confirmed
        advanceTimeBy(FREE_TIER_SETTLE_MILLIS * 4)
        runCurrent()
        assertFalse(values.last())

        tiers.emit(VerifiedTier.FREE) // server confirms the lapse
        advanceTimeBy(FREE_TIER_SETTLE_MILLIS + 1)
        runCurrent()
        assertTrue(values.last())
        job.cancel()
    }

    @Test
    fun `ads need the tier gate, UMP consent and a completed SDK initialize`() {
        assertTrue(shouldShowAds(tierAllowsAds = true, canRequestAds = true, mobileAdsReady = true))
        assertFalse(shouldShowAds(tierAllowsAds = true, canRequestAds = true, mobileAdsReady = false))
        assertFalse(shouldShowAds(tierAllowsAds = true, canRequestAds = false, mobileAdsReady = true))
        assertFalse(shouldShowAds(tierAllowsAds = false, canRequestAds = true, mobileAdsReady = true))
        assertFalse(shouldShowAds(tierAllowsAds = false, canRequestAds = false, mobileAdsReady = false))
    }
}
