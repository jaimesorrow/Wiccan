package com.jaimesorrow.wiccan.ui.settings

import app.cash.turbine.test
import com.jaimesorrow.wiccan.analytics.AnalyticsConsentManager
import com.jaimesorrow.wiccan.analytics.AnalyticsLogger
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.data.local.ThemePreferences
import com.jaimesorrow.wiccan.domain.model.AuthState
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.ProfileName
import com.jaimesorrow.wiccan.domain.model.WiccanThemeMode
import com.jaimesorrow.wiccan.domain.repository.NotificationRepository
import com.jaimesorrow.wiccan.domain.usecase.DeleteAccountUseCase
import com.jaimesorrow.wiccan.ui.auth.FakeAuthRepository
import com.jaimesorrow.wiccan.ui.paywall.FakeSubscriptionRepository
import com.jaimesorrow.wiccan.widget.WiccanWidgetUpdater
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
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
    private lateinit var themePreferences: ThemePreferences
    private lateinit var wiccanWidgetUpdater: WiccanWidgetUpdater
    private lateinit var consentManager: AnalyticsConsentManager
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAuthRepository()
        subscriptionRepository = FakeSubscriptionRepository()
        notificationRepository = mockk(relaxed = true)
        themePreferences = mockk(relaxed = true)
        wiccanWidgetUpdater = mockk(relaxed = true)
        every { themePreferences.themeMode } returns flowOf(WiccanThemeMode.DEFAULT)
        consentManager = mockk(relaxed = true)
        every { consentManager.consent } returns flowOf(false)
        coEvery { consentManager.setConsent(any()) } returns true
        val analyticsLogger: AnalyticsLogger = mockk(relaxed = true)
        viewModel = SettingsViewModel(
            repository,
            DeleteAccountUseCase(repository, analyticsLogger),
            subscriptionRepository,
            notificationRepository,
            themePreferences,
            wiccanWidgetUpdater,
            consentManager,
        )
    }

    @Test
    fun `analytics switch reflects the stored choice and records changes`() = runTest(dispatcher) {
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.shareAnalytics)

        viewModel.onEvent(SettingsUiEvent.ShareAnalyticsToggled(true))
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.shareAnalytics)
        coVerify { consentManager.setConsent(true) }

        viewModel.onEvent(SettingsUiEvent.ShareAnalyticsToggled(false))
        dispatcher.scheduler.advanceUntilIdle()
        coVerify { consentManager.setConsent(false) }
    }

    @Test
    fun `name fields are prefilled with the saved profile name`() = runTest(dispatcher) {
        repository.setAuthState(AuthState.SignedIn("uid-a"))
        repository.profileName = ProfileName("Raven", "Moon")
        val viewModel = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Raven", viewModel.state.value.firstName)
        assertEquals("Moon", viewModel.state.value.lastName)
    }

    @Test
    fun `a delayed profile read never overwrites edits, even after typing then clearing`() = runTest(dispatcher) {
        repository.setAuthState(AuthState.SignedIn("uid-a"))
        repository.profileNameReader = {
            delay(5_000)
            ProfileName("Raven", "Moon")
        }
        val viewModel = newViewModel()
        dispatcher.scheduler.runCurrent()

        viewModel.onEvent(SettingsUiEvent.FirstNameChanged("Ash"))
        viewModel.onEvent(SettingsUiEvent.FirstNameChanged(""))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("", viewModel.state.value.firstName)
        assertEquals("", viewModel.state.value.lastName)
    }

    @Test
    fun `names never carry over to another account after sign-out`() = runTest(dispatcher) {
        val names = mapOf("uid-a" to ProfileName("Alice", "Ash"), "uid-b" to ProfileName("Bram", "Birch"))
        var signedIn: String? = "uid-a"
        repository.profileNameReader = { names[signedIn] }
        repository.setAuthState(AuthState.SignedIn("uid-a"))
        val viewModel = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Alice", viewModel.state.value.firstName)
        viewModel.onEvent(SettingsUiEvent.LastNameChanged("Ashby"))

        signedIn = null
        repository.setAuthState(AuthState.SignedOut)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("", viewModel.state.value.firstName)
        assertEquals("", viewModel.state.value.lastName)

        // User B signs in on the same activity-scoped ViewModel: B's own name, none of A's edits.
        signedIn = "uid-b"
        repository.setAuthState(AuthState.SignedIn("uid-b"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Bram", viewModel.state.value.firstName)
        assertEquals("Birch", viewModel.state.value.lastName)
    }

    @Test
    fun `switching accounts directly also drops the previous account's names`() = runTest(dispatcher) {
        repository.setAuthState(AuthState.SignedIn("uid-a"))
        repository.profileName = ProfileName("Alice", "Ash")
        val viewModel = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        repository.profileName = null // B has no saved name yet (onboarding)
        repository.setAuthState(AuthState.SignedIn("uid-b"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("", viewModel.state.value.firstName)
        assertEquals("", viewModel.state.value.lastName)
    }

    private fun newViewModel() = SettingsViewModel(
        repository,
        DeleteAccountUseCase(repository, mockk(relaxed = true)),
        subscriptionRepository,
        notificationRepository,
        themePreferences,
        wiccanWidgetUpdater,
        consentManager,
    )

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

        viewModel.effects.test {
            viewModel.onEvent(SettingsUiEvent.DeleteAccountClicked)
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

    @Test
    fun `NotificationsToggled optimistically updates state and forwards to the repository`() = runTest(dispatcher) {
        coEvery { notificationRepository.setNotificationsEnabled(false) } returns AppResult.Success(Unit)

        viewModel.onEvent(SettingsUiEvent.NotificationsToggled(false))

        assertFalse(viewModel.state.value.notificationsEnabled)
        dispatcher.scheduler.advanceUntilIdle()
        coVerify { notificationRepository.setNotificationsEnabled(false) }
        assertFalse(viewModel.state.value.notificationsEnabled)
    }

    @Test
    fun `NotificationsToggled reverts optimistic state and shows an error on failure`() = runTest(dispatcher) {
        coEvery { notificationRepository.setNotificationsEnabled(false) } returns AppResult.Failure(AppError.Network)

        viewModel.effects.test {
            viewModel.onEvent(SettingsUiEvent.NotificationsToggled(false))
            val effect = awaitItem()
            assertTrue(effect is SettingsUiEffect.ShowSnackbar)
            assertEquals(AppError.Network, (effect as SettingsUiEffect.ShowSnackbar).error)
            cancelAndConsumeRemainingEvents()
        }

        assertTrue(viewModel.state.value.notificationsEnabled)
    }

    @Test
    fun `ThemeModeSelected persists the chosen mode and refreshes the widget`() = runTest(dispatcher) {
        viewModel.onEvent(SettingsUiEvent.ThemeModeSelected(WiccanThemeMode.DEEP_FOREST))
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { themePreferences.setThemeMode(WiccanThemeMode.DEEP_FOREST) }
        coVerify { wiccanWidgetUpdater.updateAll() }
    }

    @Test
    fun `themeMode reflects the persisted preference stream`() = runTest(dispatcher) {
        every { themePreferences.themeMode } returns flowOf(WiccanThemeMode.ROYAL_PLUM)
        val viewModelWithPlum = SettingsViewModel(
            repository,
            DeleteAccountUseCase(repository, mockk(relaxed = true)),
            subscriptionRepository,
            notificationRepository,
            themePreferences,
            wiccanWidgetUpdater,
            consentManager,
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(WiccanThemeMode.ROYAL_PLUM, viewModelWithPlum.state.value.themeMode)
    }

    @Test
    fun `a second ConfirmDeleteAccount is ignored while deletion is already in progress`() = runTest(dispatcher) {
        viewModel.onEvent(SettingsUiEvent.ConfirmDeleteAccount)
        viewModel.onEvent(SettingsUiEvent.ConfirmDeleteAccount)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.deleteAccountCallCount)
    }
}
