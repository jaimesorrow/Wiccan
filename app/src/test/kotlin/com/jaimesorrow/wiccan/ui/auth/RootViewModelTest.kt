package com.jaimesorrow.wiccan.ui.auth

import com.jaimesorrow.wiccan.domain.model.AccountStatus
import com.jaimesorrow.wiccan.domain.model.AuthState
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.RootState
import com.jaimesorrow.wiccan.ui.paywall.FakeSubscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RootViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuthRepository
    private lateinit var subscription: FakeSubscriptionRepository
    private lateinit var viewModel: RootViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAuthRepository()
        subscription = FakeSubscriptionRepository().apply { emitTier(ContentTier.PREMIUM) }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts Loading before the auth state resolves`() {
        viewModel = RootViewModel(repository, subscription)
        assertEquals(RootState.Loading, viewModel.rootState.value)
    }

    @Test
    fun `a ready account without a subscription stays at the mandatory paywall`() = runTest(dispatcher) {
        subscription.emitTier(ContentTier.FREE)
        viewModel = RootViewModel(repository, subscription)
        repository.setAuthState(AuthState.SignedIn("uid-1"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(RootState.RequiresSubscription("uid-1"), viewModel.rootState.value)
        subscription.emitTier(ContentTier.PREMIUM)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(RootState.SignedIn("uid-1"), viewModel.rootState.value)
        subscription.emitTier(ContentTier.FREE)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(RootState.RequiresSubscription("uid-1"), viewModel.rootState.value)
    }

    @Test
    fun `an account switch removes access and sign out never leaves the shell mounted`() = runTest(dispatcher) {
        viewModel = RootViewModel(repository, subscription)
        repository.setAuthState(AuthState.SignedIn("uid-1"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(RootState.SignedIn("uid-1"), viewModel.rootState.value)
        subscription.emitTier(ContentTier.FREE)
        repository.setAuthState(AuthState.SignedIn("uid-2"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(RootState.RequiresSubscription("uid-2"), viewModel.rootState.value)
        repository.setAuthState(AuthState.SignedOut)
        subscription.emitTier(ContentTier.PREMIUM)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(RootState.SignedOut, viewModel.rootState.value)
    }

    @Test
    fun `resolves to SignedOut when there is no session`() = runTest(dispatcher) {
        viewModel = RootViewModel(repository, subscription)
        repository.setAuthState(AuthState.SignedOut)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(RootState.SignedOut, viewModel.rootState.value)
    }

    @Test
    fun `holds InitializingProfile while signed in but the profile doc doesn't exist yet`() = runTest(dispatcher) {
        repository.setAccountStatus("uid-1", AccountStatus.Initializing)
        viewModel = RootViewModel(repository, subscription)
        repository.setAuthState(AuthState.SignedIn("uid-1"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(RootState.InitializingProfile, viewModel.rootState.value)
    }

    @Test
    fun `advances to SignedIn once the profile becomes ready`() = runTest(dispatcher) {
        repository.setAccountStatus("uid-1", AccountStatus.Initializing)
        viewModel = RootViewModel(repository, subscription)
        repository.setAuthState(AuthState.SignedIn("uid-1"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(RootState.InitializingProfile, viewModel.rootState.value)

        repository.setAccountStatus("uid-1", AccountStatus.Ready)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(RootState.SignedIn("uid-1"), viewModel.rootState.value)
    }

    @Test
    fun `resolves straight to SignedIn when the profile already exists and terms are current`() = runTest(dispatcher) {
        viewModel = RootViewModel(repository, subscription)
        repository.setAuthState(AuthState.SignedIn("uid-1"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(RootState.SignedIn("uid-1"), viewModel.rootState.value)
    }

    @Test
    fun `holds RequiresPolicyAcceptance when the profile is missing current policy acceptance`() = runTest(dispatcher) {
        repository.setAccountStatus("uid-1", AccountStatus.RequiresPolicyAcceptance)
        viewModel = RootViewModel(repository, subscription)
        repository.setAuthState(AuthState.SignedIn("uid-1"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(RootState.RequiresPolicyAcceptance, viewModel.rootState.value)
    }

    @Test
    fun `advances to SignedIn once policy acceptance is recorded`() = runTest(dispatcher) {
        repository.setAccountStatus("uid-1", AccountStatus.RequiresPolicyAcceptance)
        viewModel = RootViewModel(repository, subscription)
        repository.setAuthState(AuthState.SignedIn("uid-1"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(RootState.RequiresPolicyAcceptance, viewModel.rootState.value)

        repository.setAccountStatus("uid-1", AccountStatus.Ready)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(RootState.SignedIn("uid-1"), viewModel.rootState.value)
    }
}
