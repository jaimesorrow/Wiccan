package com.wiccan.app.ui.settings

import app.cash.turbine.test
import com.wiccan.app.analytics.AnalyticsLogger
import com.wiccan.app.core.error.AppError
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.domain.model.ContentTier
import com.wiccan.app.domain.repository.NotificationRepository
import com.wiccan.app.domain.usecase.DeleteAccountUseCase
import com.wiccan.app.ui.auth.FakeAuthRepository
import com.wiccan.app.ui.paywall.FakeSubscriptionRepository
import io.mockk.coVerify
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
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuthRepository
    private lateinit var subscriptionRepository: FakeSubscriptionRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAuthRepository()
        subscriptionRepository = FakeSubscriptionRepository()
        notificationRepository = mockk(relaxed = true)
        val analyticsLogger: AnalyticsLogger = mockk(relaxed = true)
        viewModel = SettingsViewModel(
            repository,
            DeleteAccountUseCase(repository, analyticsLogger),
            subscriptionRepository,
            notificationRepository,
        )
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
    fun `SignOutClicked best-effort unregisters the device token before signing out`() = runTest(dispatcher) {
        viewModel.onEvent(SettingsUiEvent.SignOutClicked)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { notificationRepository.unregisterToken() }
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
    fun `a failed deletion forwards the repository's error and does not sign out`() = runTest(dispatcher) {
        repository.deleteAccountResult = AppResult.Failure(AppError.Network)

        viewModel.onEvent(SettingsUiEvent.DeleteAccountClicked)
        viewModel.effects.test {
            viewModel.onEvent(SettingsUiEvent.ConfirmDeleteAccount)
            val effect = awaitItem()
            assertTrue(effect is SettingsUiEffect.ShowSnackbar)
            assertEquals(AppError.Network, (effect as SettingsUiEffect.ShowSnackbar).error)
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
