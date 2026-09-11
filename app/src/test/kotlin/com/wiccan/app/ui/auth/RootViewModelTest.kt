package com.wiccan.app.ui.auth

import com.wiccan.app.domain.model.AccountStatus
import com.wiccan.app.domain.model.AuthState
import com.wiccan.app.domain.model.RootState
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
    private lateinit var viewModel: RootViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAuthRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts Loading before the auth state resolves`() {
        viewModel = RootViewModel(repository)
        assertEquals(RootState.Loading, viewModel.rootState.value)
    }

    @Test
    fun `resolves to SignedOut when there is no session`() = runTest(dispatcher) {
        viewModel = RootViewModel(repository)
        repository.setAuthState(AuthState.SignedOut)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(RootState.SignedOut, viewModel.rootState.value)
    }

    @Test
    fun `holds InitializingProfile while signed in but the profile doc doesn't exist yet`() = runTest(dispatcher) {
        repository.setAccountStatus("uid-1", AccountStatus.Initializing)
        viewModel = RootViewModel(repository)
        repository.setAuthState(AuthState.SignedIn("uid-1"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(RootState.InitializingProfile, viewModel.rootState.value)
    }

    @Test
    fun `advances to SignedIn once the profile becomes ready`() = runTest(dispatcher) {
        repository.setAccountStatus("uid-1", AccountStatus.Initializing)
        viewModel = RootViewModel(repository)
        repository.setAuthState(AuthState.SignedIn("uid-1"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(RootState.InitializingProfile, viewModel.rootState.value)

        repository.setAccountStatus("uid-1", AccountStatus.Ready)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(RootState.SignedIn, viewModel.rootState.value)
    }

    @Test
    fun `resolves straight to SignedIn when the profile already exists and terms are current`() = runTest(dispatcher) {
        viewModel = RootViewModel(repository)
        repository.setAuthState(AuthState.SignedIn("uid-1"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(RootState.SignedIn, viewModel.rootState.value)
    }

    @Test
    fun `holds RequiresPolicyAcceptance when the profile is missing current policy acceptance`() = runTest(dispatcher) {
        repository.setAccountStatus("uid-1", AccountStatus.RequiresPolicyAcceptance)
        viewModel = RootViewModel(repository)
        repository.setAuthState(AuthState.SignedIn("uid-1"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(RootState.RequiresPolicyAcceptance, viewModel.rootState.value)
    }

    @Test
    fun `advances to SignedIn once policy acceptance is recorded`() = runTest(dispatcher) {
        repository.setAccountStatus("uid-1", AccountStatus.RequiresPolicyAcceptance)
        viewModel = RootViewModel(repository)
        repository.setAuthState(AuthState.SignedIn("uid-1"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(RootState.RequiresPolicyAcceptance, viewModel.rootState.value)

        repository.setAccountStatus("uid-1", AccountStatus.Ready)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(RootState.SignedIn, viewModel.rootState.value)
    }
}
