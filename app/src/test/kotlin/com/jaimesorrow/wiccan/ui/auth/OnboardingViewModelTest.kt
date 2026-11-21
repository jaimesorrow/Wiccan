package com.jaimesorrow.wiccan.ui.auth

import com.jaimesorrow.wiccan.domain.model.AuthState
import com.wiccan.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = OnboardingViewModel(FakeAuthRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `verification notice is cleared when the name step is shown`() = runTest(dispatcher) {
        viewModel.onStepShown(OnboardingStep.VERIFY_EMAIL)
        viewModel.checkVerification()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(R.string.onboarding_notice_not_verified, viewModel.state.value.notice)

        viewModel.onStepShown(OnboardingStep.PROFILE)

        assertEquals(OnboardingStep.PROFILE, viewModel.state.value.step)
        assertNull(viewModel.state.value.notice)
    }

    @Test
    fun `a result that lands after the step changed is not shown on the new step`() = runTest(dispatcher) {
        viewModel.onStepShown(OnboardingStep.VERIFY_EMAIL)
        viewModel.checkVerification()
        // Root moves to the name step before the refresh call returns.
        viewModel.onStepShown(OnboardingStep.PROFILE)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.state.value.notice)
        assertEquals(false, viewModel.state.value.busy)
    }

    @Test
    fun `notice is only exposed for the step it was raised on`() {
        val state = OnboardingUiState(step = OnboardingStep.VERIFY_EMAIL, notice = R.string.onboarding_notice_resent)
        assertEquals(R.string.onboarding_notice_resent, state.noticeFor(OnboardingStep.VERIFY_EMAIL))
        assertNull(state.noticeFor(OnboardingStep.PROFILE))
    }

    @Test
    fun `re-reporting the same step keeps its notice`() = runTest(dispatcher) {
        viewModel.onStepShown(OnboardingStep.PROFILE)
        viewModel.completeProfile() // blank names -> validation notice
        viewModel.onStepShown(OnboardingStep.PROFILE)
        assertEquals(R.string.onboarding_notice_name_invalid, viewModel.state.value.notice)
    }

    @Test
    fun `names and notices are cleared when the account changes`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        repository.setAuthState(AuthState.SignedIn("uid-a"))
        val viewModel = OnboardingViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onStepShown(OnboardingStep.PROFILE)
        viewModel.setFirstName("Alice")
        viewModel.setLastName("Ash")
        viewModel.completeProfile()
        dispatcher.scheduler.advanceUntilIdle()

        repository.setAuthState(AuthState.SignedOut)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("", state.firstName)
        assertEquals("", state.lastName)
        assertNull(state.notice)
        assertEquals(OnboardingStep.PROFILE, state.step)
    }
}
