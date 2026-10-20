package com.jaimesorrow.wiccan.ui.auth

import android.content.Context
import app.cash.turbine.test
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.result.AppResult
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
    fun toggleModeSwitchesBetweenSignInAndCreateAccount() {
        assertEquals(SignInMode.SIGN_IN, viewModel.state.value.mode)

        viewModel.onEvent(SignInUiEvent.ToggleMode)
        assertEquals(SignInMode.CREATE_ACCOUNT, viewModel.state.value.mode)

        viewModel.onEvent(SignInUiEvent.ToggleMode)
        assertEquals(SignInMode.SIGN_IN, viewModel.state.value.mode)
    }

    @Test
    fun emptyCredentialsShowFieldValidationAndDoNotCallRepository() = runTest {
        viewModel.onEvent(SignInUiEvent.SubmitEmailAuth)

        assertEquals("Enter your email address.", viewModel.state.value.emailError)
        assertEquals("Enter your password.", viewModel.state.value.passwordError)
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(null, repository.lastSignInEmail)
        assertEquals(null, repository.lastCreateAccountEmail)
    }

    @Test
    fun invalidEmailAndShortPasswordShowFieldValidation() = runTest {
        viewModel.onEvent(SignInUiEvent.EmailChanged("not-an-email"))
        viewModel.onEvent(SignInUiEvent.PasswordChanged("short"))

        viewModel.onEvent(SignInUiEvent.SubmitEmailAuth)

        assertEquals("Enter a valid email address.", viewModel.state.value.emailError)
        assertEquals("Password must be at least 6 characters.", viewModel.state.value.passwordError)
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(null, repository.lastSignInEmail)
    }

    @Test
    fun submitEmailAuthInSignInModeCallsSignInWithEmailWithCurrentFields() = runTest(dispatcher) {
        viewModel.onEvent(SignInUiEvent.EmailChanged("witch@example.com"))
        viewModel.onEvent(SignInUiEvent.PasswordChanged("hunter2"))

        viewModel.onEvent(SignInUiEvent.SubmitEmailAuth)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("witch@example.com", repository.lastSignInEmail)
        assertEquals("hunter2", repository.lastSignInPassword)
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun submitEmailAuthInCreateAccountModeCallsCreateAccountWithEmail() = runTest(dispatcher) {
        viewModel.onEvent(SignInUiEvent.ToggleMode)
        viewModel.onEvent(SignInUiEvent.EmailChanged("new@example.com"))
        viewModel.onEvent(SignInUiEvent.PasswordChanged("hunter2"))

        viewModel.onEvent(SignInUiEvent.SubmitEmailAuth)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("new@example.com", repository.lastCreateAccountEmail)
    }

    @Test
    fun aFailedSignInForwardsTheRepositoryError() = runTest(dispatcher) {
        repository.emailSignInResult = AppResult.Failure(AppError.Unknown("bad credentials"))

        viewModel.effects.test {
            viewModel.onEvent(SignInUiEvent.EmailChanged("witch@example.com"))
            viewModel.onEvent(SignInUiEvent.PasswordChanged("hunter2"))
            viewModel.onEvent(SignInUiEvent.SubmitEmailAuth)
            val effect = awaitItem()
            assertTrue(effect is SignInUiEffect.ShowSnackbar)
            assertEquals(AppError.Unknown("bad credentials"), (effect as SignInUiEffect.ShowSnackbar).error)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun forgotPasswordClickedSendsAResetEmailAndEmitsAConfirmationEffect() = runTest {
        viewModel.onEvent(SignInUiEvent.EmailChanged("forgot@example.com"))

        viewModel.effects.test {
            viewModel.onEvent(SignInUiEvent.ForgotPasswordClicked)
            assertEquals(SignInUiEffect.PasswordResetEmailSent, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
        assertEquals("forgot@example.com", repository.lastPasswordResetEmail)
    }

    @Test
    fun googleSignInIsOfferedWhenTheBuildHasAWebOAuthClient() {
        assertTrue(viewModel.state.value.isGoogleSignInAvailable)
    }

    @Test
    fun googleSignInIsHiddenWhenTheBuildHasNoWebOAuthClient() {
        repository.googleSignInAvailable = false

        assertFalse(SignInViewModel(repository).state.value.isGoogleSignInAvailable)
    }

    @Test
    fun emailSignInWorksWhenGoogleSignInIsUnavailable() = runTest(dispatcher) {
        repository.googleSignInAvailable = false
        viewModel = SignInViewModel(repository)
        viewModel.onEvent(SignInUiEvent.EmailChanged("witch@example.com"))
        viewModel.onEvent(SignInUiEvent.PasswordChanged("hunter2"))

        viewModel.onEvent(SignInUiEvent.SubmitEmailAuth)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isGoogleSignInAvailable)
        assertEquals("witch@example.com", repository.lastSignInEmail)
        assertEquals("hunter2", repository.lastSignInPassword)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun googleSignInFailureEmitsASnackbarEffect() = runTest(dispatcher) {
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
