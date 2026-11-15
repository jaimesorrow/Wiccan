package com.jaimesorrow.wiccan.ui.today

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.jaimesorrow.wiccan.data.mapper.toContent
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class TodayScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `exhausted inventory has an explicit state that points to Grimoire`() {
        var openedGrimoire = false
        composeRule.setContent {
            MaterialTheme {
                TodayScreenContent(
                    state = TodayUiState(isLoading = false, poolExhausted = true),
                    effects = emptyFlow(),
                    onEvent = {},
                    onOpenGrimoire = { openedGrimoire = true },
                )
            }
        }
        composeRule.onNodeWithText("The lessons have all been gathered").assertIsDisplayed()
        composeRule.onNodeWithTag("today-empty-logo").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Every lesson we hold is already yours. They wait in your Grimoire.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Open your Grimoire").performClick()
        assertTrue(openedGrimoire)
        composeRule.onNodeWithText("Beginner").assertDoesNotExist()
        composeRule.onNodeWithText("Intermediate").assertDoesNotExist()
        composeRule.onNodeWithText("Advanced").assertDoesNotExist()
    }

    @Test
    fun `delivered lesson displays the approved structure and opens Grimoire without preference questions`() {
        val lesson = mapOf<String, Any?>(
            "id" to "lesson-1",
            "type" to "lesson",
            "tier" to "free",
            "title" to "Seasonal observation",
            "tradition" to "Contemporary Wicca",
            "category" to "MoonAndSeasonal",
            "intro" to "Observe the changing season.",
            "body" to "Notice what has changed in your surroundings.",
            "reflection" to "What did you notice?",
            "practice" to "Sit quietly and breathe.",
            "safetyNote" to "Stay within comfortable movement and breathing.",
            "sourceCitation" to "Modern Wiccan practice guide",
            "rightsNotes" to "Original educational synthesis",
        ).toContent()
        var openedGrimoire = false
        composeRule.setContent {
            MaterialTheme {
                TodayScreenContent(
                    state = TodayUiState(isLoading = false, content = lesson),
                    effects = emptyFlow(),
                    onEvent = {},
                    onOpenGrimoire = { openedGrimoire = true },
                )
            }
        }
        composeRule.onNodeWithText("Today's lesson").assertIsDisplayed()
        composeRule.onNodeWithText("The moon has turned. Your next practice awaits.").assertIsDisplayed()
        composeRule.onNodeWithText(lesson.title).assertIsDisplayed()
        composeRule.onNodeWithText("What did you notice?").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sit quietly and breathe.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Modern Wiccan practice guide").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Original educational synthesis").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Kept in your Grimoire").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Open your Grimoire").performScrollTo().performClick()
        assertTrue(openedGrimoire)
        composeRule.onNodeWithText("Beginner").assertDoesNotExist()
        composeRule.onNodeWithText("Intermediate").assertDoesNotExist()
        composeRule.onNodeWithText("Advanced").assertDoesNotExist()
    }
}
