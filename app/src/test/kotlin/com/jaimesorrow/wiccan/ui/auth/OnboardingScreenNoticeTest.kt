package com.jaimesorrow.wiccan.ui.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
class OnboardingScreenNoticeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `verification message does not carry over to the name step`() {
        val viewModel = OnboardingViewModel(FakeAuthRepository())
        var verification by mutableStateOf(true)
        composeRule.setContent {
            MaterialTheme { OnboardingScreen(verification = verification, viewModel = viewModel) }
        }

        composeRule.onNodeWithText("I opened the moonlit link").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Verification checked. Confirm your email before continuing.")
            .performScrollTo().assertIsDisplayed()

        // The root state machine moves on once the refreshed token shows the email verified.
        verification = false
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("onboarding-notice").assertDoesNotExist()
        composeRule.onNodeWithText("Verification checked. Confirm your email before continuing.")
            .assertDoesNotExist()
    }
}
