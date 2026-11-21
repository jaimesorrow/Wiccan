package com.jaimesorrow.wiccan.ui.auth

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.jaimesorrow.wiccan.domain.model.AuthState
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The sign-in screen's ViewModel is activity-scoped, so it is the same
 * instance before sign-up and after a later sign-out (emulator re-test,
 * screenshot 34). Drives the real screen + ViewModel through that cycle.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class SignOutLandsOnSignInTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `after sign-out the Sign In screen shows, with no previous email, and Back isn't intercepted`() {
        val auth = FakeAuthRepository()
        val viewModel = SignInViewModel(auth)
        composeRule.setContent { MaterialTheme { SignInScreen(viewModel = viewModel) } }

        composeRule.onNodeWithText("New to the path? Create an account").performScrollTo().performClick()
        composeRule.onNodeWithText("Begin your path.").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction() and hasText("Email")).performTextInput("previous@example.com")
        composeRule.onNodeWithText("previous@example.com").assertIsDisplayed()

        // Account created and signed in, later signed out (root shows SignInScreen again).
        composeRule.runOnIdle { auth.setAuthState(AuthState.SignedIn("uid-a")) }
        composeRule.waitForIdle()
        composeRule.runOnIdle { auth.setAuthState(AuthState.SignedOut) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Welcome back.").assertIsDisplayed()
        composeRule.onNodeWithText("Begin your path.").assertDoesNotExist()
        composeRule.onNodeWithText("previous@example.com").assertDoesNotExist()
        // Nothing on the signed-out screen handles Back, so it leaves the app
        // instead of returning to a signed-in screen.
        composeRule.runOnIdle {
            assertFalse(composeRule.activity.onBackPressedDispatcher.hasEnabledCallbacks())
        }
    }
}
