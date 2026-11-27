package com.jaimesorrow.wiccan.ads

import com.jaimesorrow.wiccan.domain.model.ContentTier
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
        val values = adsAllowedForTier(flowOf(ContentTier.PREMIUM)).toList()
        assertEquals(listOf(false), values)
    }

    @Test
    fun `free shows ads only after the tier settles`() = runTest {
        val tiers = MutableSharedFlow<ContentTier>()
        val values = mutableListOf<Boolean>()
        val job = launch { adsAllowedForTier(tiers).collect { values += it } }
        runCurrent()
        tiers.emit(ContentTier.FREE)
        advanceTimeBy(FREE_TIER_SETTLE_MILLIS - 1)
        runCurrent()
        assertEquals(listOf(false), values)
        advanceTimeBy(2)
        runCurrent()
        assertEquals(listOf(false, true), values)
        job.cancel()
    }

    @Test
    fun `cached free followed by verified premium never flashes an ad`() = runTest {
        val tiers = MutableSharedFlow<ContentTier>()
        val values = mutableListOf<Boolean>()
        val job = launch { adsAllowedForTier(tiers).collect { values += it } }
        runCurrent()
        tiers.emit(ContentTier.FREE)
        advanceTimeBy(FREE_TIER_SETTLE_MILLIS / 2)
        tiers.emit(ContentTier.PREMIUM)
        advanceTimeBy(FREE_TIER_SETTLE_MILLIS * 4)
        runCurrent()
        assertFalse(values.contains(true))
        job.cancel()
    }

    @Test
    fun `subscribing hides ads immediately and a lapse brings them back`() = runTest {
        val tiers = MutableSharedFlow<ContentTier>()
        val values = mutableListOf<Boolean>()
        val job = launch { adsAllowedForTier(tiers).collect { values += it } }
        runCurrent()
        tiers.emit(ContentTier.FREE)
        advanceTimeBy(FREE_TIER_SETTLE_MILLIS + 1)
        runCurrent()
        assertTrue(values.last())

        tiers.emit(ContentTier.PREMIUM) // monthly or yearly purchase verified
        runCurrent()
        assertFalse(values.last())

        tiers.emit(ContentTier.FREE) // subscription expired
        advanceTimeBy(FREE_TIER_SETTLE_MILLIS + 1)
        runCurrent()
        assertTrue(values.last())
        job.cancel()
    }

    @Test
    fun `ads need both the tier gate and UMP consent`() {
        assertTrue(shouldShowAds(tierAllowsAds = true, canRequestAds = true))
        assertFalse(shouldShowAds(tierAllowsAds = true, canRequestAds = false))
        assertFalse(shouldShowAds(tierAllowsAds = false, canRequestAds = true))
        assertFalse(shouldShowAds(tierAllowsAds = false, canRequestAds = false))
    }
}
