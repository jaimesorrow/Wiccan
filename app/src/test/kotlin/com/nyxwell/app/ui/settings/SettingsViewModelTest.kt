package com.nyxwell.app.ui.settings

import app.cash.turbine.test
import com.nyxwell.app.core.error.AppError
import com.nyxwell.app.core.result.AppResult
import com.nyxwell.app.domain.model.ContentTier
import com.nyxwell.app.domain.usecase.DeleteAccountUseCase
import com.nyxwell.app.ui.auth.FakeAuthRepository
import com.nyxwell.app.ui.paywall.FakeSubscriptionRepository
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
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuthRepository
    private lateinit var subscriptionRepository: FakeSubscriptionRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAuthRepository()
        subscriptionRepository = FakeSubscriptionRepository()
        viewModel = SettingsViewModel(repository, DeleteAccountUseCase(repository), subscriptionRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `SignOutClicked signs the user out`() = runTest(dispatcher) {
        viewModel.onEvent(SettingsUiEvent.SignOutClicked)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.signOutCallCount)
    }

    @Test
    fun `DeleteAccountClicked shows the confirmation dialog without deleting`() {
        viewModel.onEvent(SettingsUiEvent.DeleteAccountClicked)

        assertTrue(viewModel.state.value.showDeleteConfirmation)
        assertEquals(0, repository.deleteAccountCallCount)
    }

    @Test
    fun `CancelDeleteAccount dismisses the dialog without deleting`() {
        viewModel.onEvent(SettingsUiEvent.DeleteAccountClicked)
        viewModel.onEvent(SettingsUiEvent.CancelDeleteAccount)

        assertFalse(viewModel.state.value.showDeleteConfirmation)
        assertEquals(0, repository.deleteAccountCallCount)
    }

    @Test
    fun `ConfirmDeleteAccount deletes the account and signs out on success`() = runTest(dispatcher) {
        viewModel.onEvent(SettingsUiEvent.DeleteAccountClicked)
        viewModel.onEvent(SettingsUiEvent.ConfirmDeleteAccount)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.deleteAccountCallCount)
        assertEquals(1, repository.signOutCallCount)
        assertFalse(viewModel.state.value.isDeleting)
        assertFalse(viewModel.state.value.showDeleteConfirmation)
    }

    @Test
    fun `a failed deletion emits a DeleteAccountFailed snackbar and does not sign out`() = runTest(dispatcher) {
        val cause = IllegalStateException("network error")
        repository.deleteAccountResult = AppResult.Error(cause)

        viewModel.onEvent(SettingsUiEvent.DeleteAccountClicked)
        viewModel.effects.test {
            viewModel.onEvent(SettingsUiEvent.ConfirmDeleteAccount)
            val effect = awaitItem()
            assertTrue(effect is SettingsUiEffect.ShowSnackbar)
            val error = (effect as SettingsUiEffect.ShowSnackbar).error
            assertTrue(error is AppError.DeleteAccountFailed)
            assertEquals(cause, (error as AppError.DeleteAccountFailed).cause)
            cancelAndConsumeRemainingEvents()
        }

        assertEquals(0, repository.signOutCallCount)
        assertFalse(viewModel.state.value.isDeleting)
    }

    @Test
    fun `tier reflects the subscription repository's observeTier stream`() = runTest(dispatcher) {
        assertEquals(ContentTier.FREE, viewModel.state.value.tier)

        subscriptionRepository.emitTier(ContentTier.PREMIUM)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ContentTier.PREMIUM, viewModel.state.value.tier)
    }
}
