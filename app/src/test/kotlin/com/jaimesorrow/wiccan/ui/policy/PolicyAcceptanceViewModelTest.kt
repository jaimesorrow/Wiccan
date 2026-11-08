package com.jaimesorrow.wiccan.ui.policy

import app.cash.turbine.test
import com.jaimesorrow.wiccan.analytics.AnalyticsConsentManager
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.ui.auth.FakeAuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class PolicyAcceptanceViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuthRepository
    private lateinit var consentManager: AnalyticsConsentManager
    private lateinit var viewModel: PolicyAcceptanceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAuthRepository()
        consentManager = mockk(relaxed = true)
        every { consentManager.consent } returns flowOf(false)
        coEvery { consentManager.setConsent(any()) } returns true
        viewModel = PolicyAcceptanceViewModel(repository, consentManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun agreeToAll() {
        PolicyAgreement.entries.forEach { viewModel.onEvent(PolicyAcceptanceUiEvent.AgreementChanged(it, true)) }
    }

    @Test
    fun `AgreementChanged toggles each agreement independently`() {
        viewModel.onEvent(PolicyAcceptanceUiEvent.AgreementChanged(PolicyAgreement.ADULT, true))
        assertEquals(setOf(PolicyAgreement.ADULT), viewModel.state.value.agreed)

        viewModel.onEvent(PolicyAcceptanceUiEvent.AgreementChanged(PolicyAgreement.ADULT, false))
        assertTrue(viewModel.state.value.agreed.isEmpty())
    }

    @Test
    fun `Continue is blocked until every agreement is checked`() = runTest(dispatcher) {
        PolicyAgreement.entries.forEach { missing ->
            PolicyAgreement.entries.forEach {
                viewModel.onEvent(PolicyAcceptanceUiEvent.AgreementChanged(it, it != missing))
            }
            assertFalse("Continue enabled without $missing", viewModel.state.value.canContinue)
            viewModel.onEvent(PolicyAcceptanceUiEvent.ContinueClicked)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals("Accepted without $missing", 0, repository.acceptTermsCallCount)
        }

        viewModel.onEvent(PolicyAcceptanceUiEvent.AgreementChanged(PolicyAgreement.entries.last(), true))
        assertTrue(viewModel.state.value.canContinue)
    }

    @Test
    fun `the no-guarantee acknowledgement is one of the required agreements`() {
        assertTrue(PolicyAgreement.NO_GUARANTEE in PolicyAgreement.entries)
        assertTrue(PolicyAgreement.NO_GUARANTEE.label().contains("does not promise or guarantee any result"))
        assertTrue(PolicyAgreement.ADULT.label().contains("18"))
    }

    @Test
    fun `ContinueClicked accepts terms and clears submitting on success`() = runTest(dispatcher) {
        agreeToAll()
        viewModel.onEvent(PolicyAcceptanceUiEvent.ContinueClicked)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.acceptTermsCallCount)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun `a failed acceptance forwards the repository's error and clears submitting`() = runTest(dispatcher) {
        repository.acceptTermsResult = AppResult.Failure(AppError.Network)
        agreeToAll()

        viewModel.effects.test {
            viewModel.onEvent(PolicyAcceptanceUiEvent.ContinueClicked)
            val effect = awaitItem()
            assertTrue(effect is PolicyAcceptanceUiEffect.ShowSnackbar)
            assertEquals(AppError.Network, (effect as PolicyAcceptanceUiEffect.ShowSnackbar).error)
            cancelAndConsumeRemainingEvents()
        }
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun `analytics sharing is optional and off by default`() = runTest(dispatcher) {
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.shareAnalytics)

        agreeToAll()
        assertTrue(viewModel.state.value.canContinue)
        viewModel.onEvent(PolicyAcceptanceUiEvent.ContinueClicked)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { consentManager.setConsent(false) }
        assertEquals(1, repository.acceptTermsCallCount)
    }

    @Test
    fun `opting in records consent before accepting the policies`() = runTest(dispatcher) {
        agreeToAll()
        viewModel.onEvent(PolicyAcceptanceUiEvent.ShareAnalyticsChanged(true))
        viewModel.onEvent(PolicyAcceptanceUiEvent.ContinueClicked)
        dispatcher.scheduler.advanceUntilIdle()

        coVerifyOrder { consentManager.setConsent(true) }
        assertEquals(1, repository.acceptTermsCallCount)
    }

    @Test
    fun `a returning user's earlier opt-in is pre-selected`() = runTest(dispatcher) {
        every { consentManager.consent } returns flowOf(true)
        val returning = PolicyAcceptanceViewModel(repository, consentManager)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(returning.state.value.shareAnalytics)
    }

    @Test
    fun `no consent is recorded while a required agreement is missing`() = runTest(dispatcher) {
        viewModel.onEvent(PolicyAcceptanceUiEvent.ShareAnalyticsChanged(true))
        viewModel.onEvent(PolicyAcceptanceUiEvent.ContinueClicked)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { consentManager.setConsent(any()) }
    }

    @Test
    fun `a late stored value never overrides the user's own analytics choice`() = runTest(dispatcher) {
        every { consentManager.consent } returns flowOf(true)
        val returning = PolicyAcceptanceViewModel(repository, consentManager)
        returning.onEvent(PolicyAcceptanceUiEvent.ShareAnalyticsChanged(false))
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(returning.state.value.shareAnalytics)
    }

    @Test
    fun `a failed analytics save still accepts the policies and tells the user`() = runTest(dispatcher) {
        coEvery { consentManager.setConsent(any()) } returns false
        agreeToAll()
        viewModel.effects.test {
            viewModel.onEvent(PolicyAcceptanceUiEvent.ContinueClicked)
            assertTrue(awaitItem() is PolicyAcceptanceUiEffect.ShowSnackbar)
            cancelAndConsumeRemainingEvents()
        }
        assertEquals(1, repository.acceptTermsCallCount)
    }
}
