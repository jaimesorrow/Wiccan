package com.jaimesorrow.wiccan.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.rememberNavController
import com.jaimesorrow.wiccan.domain.model.AuthState
import com.jaimesorrow.wiccan.domain.model.RootState
import com.jaimesorrow.wiccan.ui.auth.FakeAuthRepository
import com.jaimesorrow.wiccan.ui.auth.SignInScreen
import com.jaimesorrow.wiccan.ui.auth.SignInViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Drives MainActivity's root routing ([RootRouter]) with the real signed-in
 * nav graph ([WiccanAppShellContent], stub screens) and the real sign-in
 * screen/ViewModel: navigate deep into the signed-in shell, sign out, and
 * check Sign In shows, Back can't return into the shell, and the next
 * sign-in starts on Today with a fresh back stack.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RootRouterSignOutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val stubScreens = WiccanNavScreens(
        today = { onOpenGrimoire ->
            Column {
                Text("screen:today")
                Button(onClick = onOpenGrimoire) { Text("stub:open-grimoire") }
            }
        },
        archive = { onNavigateToDetail ->
            Column {
                Text("screen:archive")
                Button(onClick = { onNavigateToDetail("lesson-42") }) { Text("stub:open-entry") }
            }
        },
        settings = { Text("screen:settings") },
        archiveDetail = { contentId, _ -> Text("screen:detail:$contentId") },
        legalDocument = { documentId, _ -> Text("screen:legal:$documentId") },
    )

    private fun click(text: String) {
        composeRule.onNodeWithText(text).performClick()
        composeRule.waitForIdle()
    }

    private fun backIsHandledInApp(): Boolean {
        var handled = false
        composeRule.runOnIdle { handled = composeRule.activity.onBackPressedDispatcher.hasEnabledCallbacks() }
        return handled
    }

    @Test
    fun `sign-out from deep in the shell lands on Sign In and Back cannot return to signed-in screens`() {
        val auth = FakeAuthRepository()
        val signInViewModel = SignInViewModel(auth)
        val rootState = MutableStateFlow<RootState>(RootState.SignedOut)
        composeRule.setContent {
            val state by rootState.collectAsState()
            MaterialTheme {
                RootRouter(
                    rootState = state,
                    signedIn = { WiccanAppShellContent(stubScreens, rememberNavController()) },
                    signedOut = { SignInScreen(viewModel = signInViewModel) },
                    other = {},
                )
            }
        }
        // Signed out at launch, then the user had opened Create Account before signing up.
        composeRule.onNodeWithText("New to the path? Create an account").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Begin your path.").assertExists()

        composeRule.runOnIdle {
            auth.setAuthState(AuthState.SignedIn("uid-a"))
            rootState.value = RootState.SignedIn("uid-a")
        }
        composeRule.waitForIdle()
        click("stub:open-grimoire")
        click("stub:open-entry")
        composeRule.onNodeWithText("screen:detail:lesson-42").assertExists()
        assertTrue("the shell's NavHost handles Back while signed in", backIsHandledInApp())

        composeRule.runOnIdle {
            auth.setAuthState(AuthState.SignedOut)
            rootState.value = RootState.SignedOut
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Welcome back.").assertExists()
        composeRule.onNodeWithText("Begin your path.").assertDoesNotExist()
        composeRule.onNodeWithText("screen:detail:lesson-42").assertDoesNotExist()
        assertFalse("no signed-in back stack is left to return to", backIsHandledInApp())

        // Next sign-in: a fresh shell on Today, not the old detail screen.
        composeRule.runOnIdle {
            auth.setAuthState(AuthState.SignedIn("uid-a"))
            rootState.value = RootState.SignedIn("uid-a")
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("screen:today").assertExists()
        composeRule.onNodeWithText("screen:detail:lesson-42").assertDoesNotExist()
    }
}
