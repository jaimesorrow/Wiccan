package com.jaimesorrow.wiccan.ui

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
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
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** The sign-in logo specifically: the startup loading screen also shows the logo. */
    private fun waitForSignIn() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasTestTag(SIGN_IN_LOGO_TAG)).fetchSemanticsNodes().isNotEmpty()
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
        waitForSignIn()
        composeRule.onNode(hasText("Terms") and hasClickAction()).assertExists()
        composeRule.onNode(hasText("Privacy") and hasClickAction()).assertExists()
        val submit = composeRule.onAllNodes((hasText("Sign In") or hasText("Create Account")) and hasClickAction())
        check(submit.fetchSemanticsNodes().isNotEmpty()) { "No sign-in or create-account button" }
    }

    @Test
    fun signInTermsLinkOpensTermsAndBackReturns() {
        waitForSignIn()
        clickButton("Terms")
        waitForText("No promise or guarantee of results")
        clickButton("← Back")
        waitForSignIn()
    }

    @Test
    fun signInPrivacyLinkOpensPrivacyAndSystemBackReturns() {
        waitForSignIn()
        clickButton("Privacy")
        composeRule.onNodeWithText("Privacy Policy").assertExists()
        Espresso.pressBack()
        waitForSignIn()
    }

    private companion object {
        /** Test tag of the full Wiccan logo on the sign-in screen; present in every sign-in mode. */
        const val SIGN_IN_LOGO_TAG = "auth-logo"
    }
}
