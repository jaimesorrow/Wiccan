package com.jaimesorrow.wiccan.ui.paywall

import android.app.Activity
import app.cash.turbine.test
import com.jaimesorrow.wiccan.analytics.AnalyticsLogger
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.result.AppResult
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PaywallViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeSubscriptionRepository
    private val analyticsLogger: AnalyticsLogger = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeSubscriptionRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads the runtime price on init`() = runTest(dispatcher) {
        repository.priceResult = AppResult.Success("$4.99/mo")

        val viewModel = PaywallViewModel(repository, analyticsLogger)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("$4.99/mo", viewModel.state.value.price)
        assertFalse(viewModel.state.value.isLoadingPrice)
        assertTrue(viewModel.state.value.hasFreeTrial)
    }

    @Test
    fun `an ineligible account is never promised a free trial`() = runTest(dispatcher) {
        repository.hasFreeTrial = false
        val viewModel = PaywallViewModel(repository, analyticsLogger)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.hasFreeTrial)
        assertEquals("$5.99", viewModel.state.value.price)
    }

    @Test
    fun `a failed price load forwards the repository's error and leaves price unset`() = runTest(dispatcher) {
        repository.priceResult = AppResult.Failure(AppError.Network)

        val viewModel = PaywallViewModel(repository, analyticsLogger)
        viewModel.effects.test {
            val effect = awaitItem()
            assertTrue(effect is PaywallUiEffect.ShowSnackbar)
            assertEquals(AppError.Network, (effect as PaywallUiEffect.ShowSnackbar).error)
            cancelAndConsumeRemainingEvents()
        }
        assertEquals(null, viewModel.state.value.price)
    }

    @Test
    fun `retrying a failed price query restores loading and recovers checkout price`() = runTest(dispatcher) {
        repository.priceResult = AppResult.Failure(AppError.Network)
        val viewModel = PaywallViewModel(repository, analyticsLogger)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.isLoadingPrice)
        assertEquals(null, viewModel.state.value.price)

        repository.priceResult = AppResult.Success("$4.99/mo")
        viewModel.onEvent(PaywallUiEvent.RetryPriceClicked)
        assertTrue(viewModel.state.value.isLoadingPrice)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, repository.priceCallCount)
        assertEquals("$4.99/mo", viewModel.state.value.price)
        assertFalse(viewModel.state.value.isLoadingPrice)
    }

    @Test
    fun `subscribe delegates to the repository's launchPurchaseFlow`() = runTest(dispatcher) {
        val viewModel = PaywallViewModel(repository, analyticsLogger)
        val activity = mockk<Activity>()

        viewModel.subscribe(activity)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(activity), repository.launchCalls)
    }

    @Test
    fun `a failed purchase launch is surfaced instead of crashing`() = runTest(dispatcher) {
        val error = AppError.PurchaseVerificationFailed("Play unavailable")
        repository.launchResult = AppResult.Failure(error)
        val viewModel = PaywallViewModel(repository, analyticsLogger)
        dispatcher.scheduler.advanceUntilIdle()
        val activity = mockk<Activity>()

        viewModel.effects.test {
            viewModel.subscribe(activity)
            assertEquals(PaywallUiEffect.ShowSnackbar(error), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `RestorePurchasesClicked restores and emits a confirmation effect`() = runTest(dispatcher) {
        val viewModel = PaywallViewModel(repository, analyticsLogger)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(PaywallUiEvent.RestorePurchasesClicked)
            assertEquals(PaywallUiEffect.PurchasesRestored, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
        assertEquals(1, repository.restoreCallCount)
        assertFalse(viewModel.state.value.isRestoring)
    }

    @Test
    fun `a failed restore forwards the repository's error`() = runTest(dispatcher) {
        repository.restoreResult = AppResult.Failure(AppError.PurchaseVerificationFailed("play unavailable"))
        val viewModel = PaywallViewModel(repository, analyticsLogger)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(PaywallUiEvent.RestorePurchasesClicked)
            val effect = awaitItem()
            assertTrue(effect is PaywallUiEffect.ShowSnackbar)
            assertEquals(
                AppError.PurchaseVerificationFailed("play unavailable"),
                (effect as PaywallUiEffect.ShowSnackbar).error,
            )
            cancelAndConsumeRemainingEvents()
        }
        assertFalse(viewModel.state.value.isRestoring)
    }
}
