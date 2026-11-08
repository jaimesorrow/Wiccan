package com.jaimesorrow.wiccan.ui.navigation

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Drives the real signed-in nav graph ([WiccanAppShellContent]) with
 * stand-in screens: every button a screen exposes is clicked, and the test
 * checks the destination it lands on, whether the bottom bar is shown, and
 * where back goes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class WiccanAppShellNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: NavHostController

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
        settings = { links ->
            Column {
                Text(if (links.accountOnly) "screen:account" else "screen:settings")
                Button(onClick = links.onBack) { Text("stub:back") }
                Button(onClick = links.onTermsClick) { Text("stub:terms") }
                Button(onClick = links.onPrivacyClick) { Text("stub:privacy") }
                Button(onClick = links.onContentSafetyClick) { Text("stub:safety") }
            }
        },
        archiveDetail = { contentId, onBack ->
            Column {
                Text("screen:detail:$contentId")
                Button(onClick = onBack) { Text("stub:back") }
            }
        },
        legalDocument = { documentId, onBack ->
            Column {
                Text("screen:legal:$documentId")
                Button(onClick = onBack) { Text("stub:back") }
            }
        },
    )

    private fun launch(startOnPaywall: Boolean = false) {
        composeRule.setContent {
            navController = rememberNavController()
            MaterialTheme {
                WiccanAppShellContent(stubScreens, navController, startOnPaywall = startOnPaywall)
            }
        }
    }

    private fun assertScreen(name: String) {
        composeRule.onNodeWithText("screen:$name").assertExists()
    }

    private fun assertBottomBar(visible: Boolean) {
        val node = composeRule.onNodeWithText("Grimoire")
        if (visible) node.assertExists() else node.assertDoesNotExist()
    }

    private fun click(text: String) {
        composeRule.onNodeWithText(text).performClick()
        composeRule.waitForIdle()
    }

    private fun systemBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    @Test
    fun `bottom bar switches between Today, Grimoire and Settings`() {
        launch()
        assertScreen("today")
        assertBottomBar(visible = true)

        click("Grimoire")
        assertScreen("archive")
        composeRule.onNodeWithText("Grimoire").assertIsSelected()

        click("Settings")
        assertScreen("settings")
        composeRule.onNodeWithText("Settings").assertIsSelected()

        click("Today")
        assertScreen("today")
    }

    @Test
    fun `Open Grimoire on Today goes to the Grimoire tab`() {
        launch()
        click("stub:open-grimoire")
        assertScreen("archive")
        assertBottomBar(visible = true)
    }

    @Test
    fun `a Grimoire entry opens its detail without the bottom bar and Back returns`() {
        launch()
        click("Grimoire")
        click("stub:open-entry")
        assertScreen("detail:lesson-42")
        assertBottomBar(visible = false)

        click("stub:back")
        assertScreen("archive")
        assertBottomBar(visible = true)

        click("stub:open-entry")
        systemBack()
        assertScreen("archive")
    }

    @Test
    fun `Settings legal links open each document and Back returns to Settings`() {
        launch()
        click("Settings")
        listOf(
            "stub:terms" to LegalDocumentId.TERMS,
            "stub:privacy" to LegalDocumentId.PRIVACY,
            "stub:safety" to LegalDocumentId.CONTENT_SAFETY,
        ).forEach { (button, documentId) ->
            click(button)
            assertScreen("legal:$documentId")
            assertBottomBar(visible = false)
            click("stub:back")
            assertScreen("settings")
        }
    }

    @Test
    fun `system back from a legal document returns to Settings`() {
        launch()
        click("Settings")
        click("stub:privacy")
        systemBack()
        assertScreen("settings")
    }

    @Test
    fun `premium start opens account management and its Back falls back to Today`() {
        launch(startOnPaywall = true)
        assertScreen("account")
        assertBottomBar(visible = false)

        click("stub:back")
        assertScreen("today")
        assertEquals(WiccanDestinations.TODAY, navController.currentDestination?.route)
        assertEquals(
            "Back from Today must not return to the account screen",
            1,
            navController.currentBackStack.value.count {
                it.destination.route !=
                    null
            }
        )
    }

    @Test
    fun `account management legal links work and return to the account screen`() {
        launch(startOnPaywall = true)
        click("stub:terms")
        assertScreen("legal:${LegalDocumentId.TERMS}")
        click("stub:back")
        assertScreen("account")
    }

    @Test
    fun `wiccan deep links resolve to Today and account management`() {
        launch()
        click("Settings")
        composeRule.runOnUiThread { navController.navigate(Uri.parse(WiccanDeepLinks.TODAY_URI)) }
        composeRule.waitForIdle()
        assertScreen("today")

        composeRule.runOnUiThread { navController.navigate(Uri.parse(WiccanDeepLinks.PREMIUM_URI)) }
        composeRule.waitForIdle()
        assertScreen("account")
    }

    @Test
    fun `re-selecting a tab does not stack duplicate destinations`() {
        launch()
        click("Grimoire")
        click("Grimoire")
        click("Grimoire")
        systemBack()
        assertScreen("today")
    }
}
