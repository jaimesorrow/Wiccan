package com.wiccan.app.ui.policy

import app.cash.turbine.test
import com.wiccan.app.core.error.AppError
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.ui.auth.FakeAuthRepository
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
class PolicyAcceptanceViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuthRepository
    private lateinit var viewModel: PolicyAcceptanceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAuthRepository()
        viewModel = PolicyAcceptanceViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `CheckedChanged updates the checkbox state`() {
        viewModel.onEvent(PolicyAcceptanceUiEvent.CheckedChanged(true))
        assertTrue(viewModel.state.value.isChecked)

        viewModel.onEvent(PolicyAcceptanceUiEvent.CheckedChanged(false))
        assertFalse(viewModel.state.value.isChecked)
    }

    @Test
    fun `ContinueClicked accepts terms and clears submitting on success`() = runTest(dispatcher) {
        viewModel.onEvent(PolicyAcceptanceUiEvent.CheckedChanged(true))
        viewModel.onEvent(PolicyAcceptanceUiEvent.ContinueClicked)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.acceptTermsCallCount)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun `a failed acceptance forwards the repository's error and clears submitting`() = runTest(dispatcher) {
        repository.acceptTermsResult = AppResult.Failure(AppError.Network)
        viewModel.onEvent(PolicyAcceptanceUiEvent.CheckedChanged(true))

        viewModel.effects.test {
            viewModel.onEvent(PolicyAcceptanceUiEvent.ContinueClicked)
            val effect = awaitItem()
            assertTrue(effect is PolicyAcceptanceUiEffect.ShowSnackbar)
            assertEquals(AppError.Network, (effect as PolicyAcceptanceUiEffect.ShowSnackbar).error)
            cancelAndConsumeRemainingEvents()
        }
        assertFalse(viewModel.state.value.isSubmitting)
    }
}
