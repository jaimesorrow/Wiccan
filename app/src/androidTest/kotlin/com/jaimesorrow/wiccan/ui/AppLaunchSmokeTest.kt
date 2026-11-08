package com.jaimesorrow.wiccan.ui

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jaimesorrow.wiccan.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real-device smoke test run by the Instrumented tests workflow on an
 * emulator: the production app launches signed out (built with
 * -PuseFirebaseEmulators=true, so nothing reaches production Firebase),
 * shows the sign-in screen, and its legal links open and close.
 */
@RunWith(AndroidJUnit4::class)
class AppLaunchSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText(text) or hasContentDescription(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun clickButton(text: String) {
        val node = composeRule.onNode(hasText(text) and hasClickAction())
        runCatching { node.performScrollTo() }
        node.performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun launchesToSignInWithItsActions() {
        waitForText(SIGN_IN_ANCHOR)
        composeRule.onNode(hasText("Terms") and hasClickAction()).assertExists()
        composeRule.onNode(hasText("Privacy") and hasClickAction()).assertExists()
        val submit = composeRule.onAllNodes((hasText("Sign In") or hasText("Create Account")) and hasClickAction())
        check(submit.fetchSemanticsNodes().isNotEmpty()) { "No sign-in or create-account button" }
    }

    @Test
    fun signInTermsLinkOpensTermsAndBackReturns() {
        waitForText(SIGN_IN_ANCHOR)
        clickButton("Terms")
        waitForText("No promise or guarantee of results")
        clickButton("← Back")
        waitForText(SIGN_IN_ANCHOR)
    }

    @Test
    fun signInPrivacyLinkOpensPrivacyAndSystemBackReturns() {
        waitForText(SIGN_IN_ANCHOR)
        clickButton("Privacy")
        composeRule.onNodeWithText("Privacy Policy").assertExists()
        Espresso.pressBack()
        waitForText(SIGN_IN_ANCHOR)
    }

    private companion object {
        /** The full Wiccan logo at the top of the sign-in screen; present in every sign-in mode. */
        const val SIGN_IN_ANCHOR = "Wiccan"
    }
}
