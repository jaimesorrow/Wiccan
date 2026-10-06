package com.jaimesorrow.wiccan.ui.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
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
class GrimoireNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `normal app entry goes straight to Today without a preference destination`() {
        assertEquals(WiccanDestinations.TODAY, WiccanDestinations.startDestination(startOnPaywall = false))
        assertEquals(WiccanDestinations.PAYWALL, WiccanDestinations.startDestination(startOnPaywall = true))
    }

    @Test
    fun `navigation exposes Grimoire through its accessible label and preserves archive route`() {
        var clickedRoute: String? = null
        composeRule.setContent {
            MaterialTheme { WiccanBottomBarContent(WiccanDestinations.ARCHIVE) { clickedRoute = it } }
        }
        composeRule.onNodeWithText("Grimoire").assertIsDisplayed().assertHasClickAction().assertIsSelected().performClick()
        assertEquals("archive", clickedRoute)
        composeRule.onNodeWithText("Library").assertDoesNotExist()
        composeRule.onNodeWithText("Book of Shadows").assertDoesNotExist()
    }
}
