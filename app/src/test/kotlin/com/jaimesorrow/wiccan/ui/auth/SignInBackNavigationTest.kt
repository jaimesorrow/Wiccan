package com.jaimesorrow.wiccan.ui.auth

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class SignInBackNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `system Back returns from password recovery to sign in`() {
        val viewModel = SignInViewModel(FakeAuthRepository())
        composeRule.setContent {
            MaterialTheme { SignInScreen(viewModel = viewModel) }
        }

        composeRule.onNodeWithText("Forgot password?").performScrollTo().performClick()
        composeRule.onNodeWithText("Reset your password").assertExists()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Welcome back.").assertExists()
        composeRule.onNodeWithText("Reset your password").assertDoesNotExist()
    }
}
