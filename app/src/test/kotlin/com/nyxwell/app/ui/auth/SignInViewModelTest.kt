package com.nyxwell.app.ui.auth

import android.content.Context
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuthRepository
    private lateinit var viewModel: SignInViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAuthRepository()
        viewModel = SignInViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ToggleMode switches between sign-in and create-account`() {
        assertEquals(SignInMode.SIGN_IN, viewModel.state.value.mode)

        viewModel.onEvent(SignInUiEvent.ToggleMode)
        assertEquals(SignInMode.CREATE_ACCOUNT, viewModel.state.value.mode)

        viewModel.onEvent(SignInUiEvent.ToggleMode)
        assertEquals(SignInMode.SIGN_IN, viewModel.state.value.mode)
    }

    @Test
    fun `SubmitEmailAuth in sign-in mode calls signInWithEmail with current fields`() = runTest(dispatcher) {
        viewModel.onEvent(SignInUiEvent.EmailChanged("witch@example.com"))
        viewModel.onEvent(SignInUiEvent.PasswordChanged("hunter2"))

        viewModel.onEvent(SignInUiEvent.SubmitEmailAuth)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("witch@example.com", repository.lastSignInEmail)
        assertEquals("hunter2", repository.lastSignInPassword)
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun `SubmitEmailAuth in create-account mode calls createAccountWithEmail`() = runTest(dispatcher) {
        viewModel.onEvent(SignInUiEvent.ToggleMode)
        viewModel.onEvent(SignInUiEvent.EmailChanged("new@example.com"))

        viewModel.onEvent(SignInUiEvent.SubmitEmailAuth)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("new@example.com", repository.lastCreateAccountEmail)
    }

    @Test
    fun `a failed sign-in emits an Auth snackbar effect`() = runTest(dispatcher) {
        val cause = IllegalStateException("bad credentials")
        repository.emailSignInResult = AppResult.Error(cause)

        viewModel.effects.test {
            viewModel.onEvent(SignInUiEvent.SubmitEmailAuth)
            val effect = awaitItem()
            assertTrue(effect is SignInUiEffect.ShowSnackbar)
            val error = (effect as SignInUiEffect.ShowSnackbar).error
            assertTrue(error is AppError.Auth)
            assertEquals(cause, (error as AppError.Auth).cause)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `ForgotPasswordClicked sends a reset email and emits a confirmation effect`() = runTest(dispatcher) {
        viewModel.onEvent(SignInUiEvent.EmailChanged("forgot@example.com"))

        viewModel.effects.test {
            viewModel.onEvent(SignInUiEvent.ForgotPasswordClicked)
            assertEquals(SignInUiEffect.PasswordResetEmailSent, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
        assertEquals("forgot@example.com", repository.lastPasswordResetEmail)
    }

    @Test
    fun `Google sign-in failure emits an Auth snackbar effect`() = runTest(dispatcher) {
        val cause = IllegalStateException("cancelled")
        repository.googleResult = AppResult.Error(cause)
        val context = mockk<Context>()

        viewModel.effects.test {
            viewModel.onGoogleSignInClick(context)
            val effect = awaitItem()
            assertTrue(effect is SignInUiEffect.ShowSnackbar)
            cancelAndConsumeRemainingEvents()
        }
    }
}
