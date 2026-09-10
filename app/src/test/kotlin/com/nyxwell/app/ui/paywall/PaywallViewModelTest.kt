package com.nyxwell.app.ui.paywall

import android.app.Activity
import app.cash.turbine.test
import com.nyxwell.app.core.error.AppError
import com.nyxwell.app.core.result.AppResult
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

        val viewModel = PaywallViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("$4.99/mo", viewModel.state.value.price)
        assertFalse(viewModel.state.value.isLoadingPrice)
    }

    @Test
    fun `a failed price load forwards the repository's error and leaves price unset`() = runTest(dispatcher) {
        repository.priceResult = AppResult.Failure(AppError.Network)

        val viewModel = PaywallViewModel(repository)
        viewModel.effects.test {
            val effect = awaitItem()
            assertTrue(effect is PaywallUiEffect.ShowSnackbar)
            assertEquals(AppError.Network, (effect as PaywallUiEffect.ShowSnackbar).error)
            cancelAndConsumeRemainingEvents()
        }
        assertEquals(null, viewModel.state.value.price)
    }

    @Test
    fun `subscribe delegates to the repository's launchPurchaseFlow`() = runTest(dispatcher) {
        val viewModel = PaywallViewModel(repository)
        val activity = mockk<Activity>()

        viewModel.subscribe(activity)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(activity), repository.launchCalls)
    }

    @Test
    fun `RestorePurchasesClicked restores and emits a confirmation effect`() = runTest(dispatcher) {
        val viewModel = PaywallViewModel(repository)
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
        val viewModel = PaywallViewModel(repository)
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
