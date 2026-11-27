package com.jaimesorrow.wiccan.ui.notification

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class NotificationPermissionExplanationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `frames the permission and explains settings control before continuing`() {
        var continues = 0
        var dismissals = 0
        composeRule.setContent {
            MaterialTheme {
                NotificationPermissionExplanation(
                    onContinue = { continues++ },
                    onNotNow = { dismissals++ },
                )
            }
        }

        composeRule.onNodeWithText("Let the grimoire call to you each dawn").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Allow notifications for the optional daily practice and trial-ending reminder. " +
                "You can turn them off anytime in Settings.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performClick()

        assertEquals(1, continues)
        assertEquals(0, dismissals)
    }

    @Test
    fun `allows notification setup to be deferred`() {
        var dismissals = 0
        composeRule.setContent {
            MaterialTheme {
                NotificationPermissionExplanation(
                    onContinue = {},
                    onNotNow = { dismissals++ },
                )
            }
        }

        composeRule.onNodeWithText("Not now").performClick()

        assertEquals(1, dismissals)
    }
}
