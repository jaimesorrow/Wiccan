package com.jaimesorrow.wiccan.ads

import android.app.Activity
import com.jaimesorrow.wiccan.domain.model.ContentTier
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
    private val tier = MutableStateFlow(ContentTier.FREE)
    private val consent = FakeAdConsentController()
    private val repository = mockk<SubscriptionRepository> {
        every { observeTier() } returns tier
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `active premium entitlement hides ads even with consent`() = runTest(dispatcher) {
        tier.value = ContentTier.PREMIUM
        consent.canRequestAds.value = true
        val viewModel = AdsViewModel(repository, consent)
        val job = launch { viewModel.showAds.collect {} }
        advanceTimeBy(FREE_TIER_SETTLE_MILLIS * 3)
        runCurrent()
        assertFalse(viewModel.showAds.value)
        assertFalse(viewModel.tierAllowsAds.value)
        job.cancel()
    }

    @Test
    fun `free user sees ads once consent allows requests`() = runTest(dispatcher) {
        val viewModel = AdsViewModel(repository, consent)
        val job = launch { viewModel.showAds.collect {} }
        advanceTimeBy(FREE_TIER_SETTLE_MILLIS + 1)
        runCurrent()
        assertTrue(viewModel.tierAllowsAds.value)
        assertFalse("no ad before UMP allows it", viewModel.showAds.value)

        consent.canRequestAds.value = true
        runCurrent()
        assertTrue(viewModel.showAds.value)

        tier.value = ContentTier.PREMIUM
        runCurrent()
        assertFalse("subscribing removes the ad at once", viewModel.showAds.value)
        job.cancel()
    }

    private class FakeAdConsentController : AdConsentController {
        override val canRequestAds = MutableStateFlow(false)
        override val privacyOptionsRequired: StateFlow<Boolean> = MutableStateFlow(false)
        override fun gatherConsent(activity: Activity) = Unit
        override fun showPrivacyOptionsForm(activity: Activity) = Unit
    }
}
