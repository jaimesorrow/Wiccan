package com.jaimesorrow.wiccan.ui.today

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
class HomeLocationRationaleTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `not now dismisses without continuing`() {
        var continued = false
        var dismissed = false
        composeRule.setContent {
            MaterialTheme {
                HomeLocationPermissionRationale(
                    onContinue = { continued = true },
                    onNotNow = { dismissed = true },
                )
            }
        }
        composeRule.onNodeWithText("Optional approximate location").assertIsDisplayed()
        composeRule.onNodeWithText("Not now").performClick()
        assertEquals(false, continued)
        assertEquals(true, dismissed)
    }

    @Test
    fun `continue invokes callback`() {
        var continued = false
        composeRule.setContent {
            MaterialTheme {
                HomeLocationPermissionRationale(
                    onContinue = { continued = true },
                    onNotNow = {},
                )
            }
        }
        composeRule.onNodeWithText("Continue").performClick()
        assertEquals(true, continued)
    }
}