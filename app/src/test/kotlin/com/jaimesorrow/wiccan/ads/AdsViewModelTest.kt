package com.jaimesorrow.wiccan.ads

import android.app.Activity
import com.jaimesorrow.wiccan.domain.model.VerifiedTier
import com.jaimesorrow.wiccan.domain.repository.SubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val tier = MutableStateFlow(VerifiedTier.FREE)
    private val consent = FakeAdConsentController()
    private val repository = mockk<SubscriptionRepository> {
        every { observeVerifiedTier() } returns tier
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `active premium entitlement hides ads even with consent`() = runTest(dispatcher) {
        tier.value = VerifiedTier.PREMIUM
        consent.canRequestAds.value = true
        consent.mobileAdsReady.value = true
        val viewModel = AdsViewModel(repository, consent)
        val job = launch { viewModel.showAds.collect {} }
        advanceTimeBy(FREE_TIER_SETTLE_MILLIS * 3)
        runCurrent()
        assertFalse(viewModel.showAds.value)
        assertFalse(viewModel.tierAllowsAds.value)
        job.cancel()
    }

    @Test
    fun `unverified tier shows no ads and asks for no consent`() = runTest(dispatcher) {
        tier.value = VerifiedTier.UNVERIFIED // cache, error, or 10s timeout
        consent.canRequestAds.value = true
        consent.mobileAdsReady.value = true
        val viewModel = AdsViewModel(repository, consent)
        val job = launch { viewModel.showAds.collect {} }
        advanceTimeBy(FREE_TIER_SETTLE_MILLIS * 10)
        runCurrent()
        assertFalse(viewModel.tierAllowsAds.value)
        assertFalse(viewModel.showAds.value)
        job.cancel()
    }

    @Test
    fun `no ad loads until MobileAds initialize has completed`() = runTest(dispatcher) {
        val viewModel = AdsViewModel(repository, consent)
        val job = launch { viewModel.showAds.collect {} }
        advanceTimeBy(FREE_TIER_SETTLE_MILLIS + 1)
        consent.canRequestAds.value = true
        runCurrent()
        assertFalse("UMP allowing requests is not enough", viewModel.showAds.value)

        consent.mobileAdsReady.value = true
        runCurrent()
        assertTrue(viewModel.showAds.value)
        job.cancel()
    }

    @Test
    fun `server confirmed free user sees ads once consent and SDK allow`() = runTest(dispatcher) {
        consent.mobileAdsReady.value = true
        val viewModel = AdsViewModel(repository, consent)
        val job = launch { viewModel.showAds.collect {} }
        advanceTimeBy(FREE_TIER_SETTLE_MILLIS + 1)
        runCurrent()
        assertTrue(viewModel.tierAllowsAds.value)
        assertFalse("no ad before UMP allows it", viewModel.showAds.value)

        consent.canRequestAds.value = true
        runCurrent()
        assertTrue(viewModel.showAds.value)

        tier.value = VerifiedTier.PREMIUM
        runCurrent()
        assertFalse("subscribing removes the ad at once", viewModel.showAds.value)
        job.cancel()
    }

    private class FakeAdConsentController : AdConsentController {
        override val canRequestAds = MutableStateFlow(false)
        override val mobileAdsReady = MutableStateFlow(false)
        override val privacyOptionsRequired: StateFlow<Boolean> = MutableStateFlow(false)
        override fun gatherConsent(activity: Activity) = Unit
        override fun showPrivacyOptionsForm(activity: Activity) = Unit
    }
}
