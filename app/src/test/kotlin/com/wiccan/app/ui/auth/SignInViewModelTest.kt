package com.wiccan.app.ui.auth

import android.content.Context
import app.cash.turbine.test
import com.wiccan.app.core.error.AppError
import com.wiccan.app.core.result.AppResult
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
    fun `a failed sign-in forwards the repository's error`() = runTest(dispatcher) {
        repository.emailSignInResult = AppResult.Failure(AppError.Unknown("bad credentials"))

        viewModel.effects.test {
            viewModel.onEvent(SignInUiEvent.SubmitEmailAuth)
            val effect = awaitItem()
            assertTrue(effect is SignInUiEffect.ShowSnackbar)
            assertEquals(AppError.Unknown("bad credentials"), (effect as SignInUiEffect.ShowSnackbar).error)
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
    fun `Google sign-in failure emits a snackbar effect`() = runTest(dispatcher) {
        repository.googleResult = AppResult.Failure(AppError.Unknown("cancelled"))
        val context = mockk<Context>()

        viewModel.effects.test {
            viewModel.onGoogleSignInClick(context)
            val effect = awaitItem()
            assertTrue(effect is SignInUiEffect.ShowSnackbar)
            cancelAndConsumeRemainingEvents()
        }
    }
}
