package com.jaimesorrow.wiccan.ui.today

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.jaimesorrow.wiccan.data.mapper.toContentOrNull
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
        ).toContentOrNull()!!
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

    @Test
    fun `Home shows the assigned spell and invokes the open action without a retry event`() {
        val spell = mapOf<String, Any?>(
            "id" to "spell-1",
            "type" to "spell",
            "tier" to "premium",
            "title" to "Closing the Ford",
            "tradition" to "Contemporary Wicca",
            "category" to "altars-sacred-space",
            "intro" to "Prepare your working space.",
            "body" to "Place a bowl inside the circle.",
        ).toContentOrNull()!!
        var opened = false
        val events = mutableListOf<TodayUiEvent>()
        composeRule.setContent {
            MaterialTheme {
                TodayScreenContent(
                    state = TodayUiState(isLoading = false, content = spell),
                    effects = emptyFlow(),
                    onEvent = { events += it },
                    onOpenToday = { opened = true },
                )
            }
        }
        composeRule.onNodeWithText(spell.title).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(spell.body).assertDoesNotExist()
        composeRule.onNodeWithText("Open today's spell").performScrollTo().performClick()
        assertTrue(opened)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `a failed load shows a retry panel instead of a blank screen`() {
        val events = mutableListOf<TodayUiEvent>()
        var openedGrimoire = false
        composeRule.setContent {
            MaterialTheme {
                TodayScreenContent(
                    state = TodayUiState(isLoading = false, loadFailed = true),
                    effects = emptyFlow(),
                    onEvent = { events += it },
                    onOpenGrimoire = { openedGrimoire = true },
                )
            }
        }
        composeRule.onNodeWithTag("today-error").assertIsDisplayed()
        composeRule.onNodeWithTag("today-retry").performScrollTo().performClick()
        assertTrue(TodayUiEvent.Retry in events)
        composeRule.onNodeWithText("Open your Grimoire").performScrollTo().performClick()
        assertTrue(openedGrimoire)
    }

    @Test
    fun `check again keeps the scroll position instead of jumping to the top`() {
        var state by mutableStateOf(TodayUiState(isLoading = false, poolExhausted = true))
        composeRule.setContent {
            MaterialTheme {
                // A short viewport, like 480x800 at font scale 1.5, so the actions start below the fold.
                Box(modifier = Modifier.size(width = 320.dp, height = 260.dp)) {
                    TodayScreenContent(
                        state = state,
                        effects = emptyFlow(),
                        onEvent = { if (it == TodayUiEvent.Retry) state = state.copy(isLoading = true) },
                    )
                }
            }
        }
        composeRule.onNodeWithTag("today-check-again").assertIsNotDisplayed()
        composeRule.onNodeWithTag("today-check-again").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("today-check-again").performClick()
        composeRule.waitForIdle()
        // While refreshing, the panel stays (no skeleton swap) and the button is disabled.
        composeRule.onNodeWithTag("today-check-again").assertIsDisplayed().assertIsNotEnabled()

        // Refresh finishes: still no new lessons.
        state = state.copy(isLoading = false)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("today-check-again").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun `retrying from the error panel keeps the panel and its scroll position`() {
        var state by mutableStateOf(TodayUiState(isLoading = false, loadFailed = true))
        composeRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.size(width = 320.dp, height = 260.dp)) {
                    TodayScreenContent(
                        state = state,
                        effects = emptyFlow(),
                        onEvent = { if (it == TodayUiEvent.Retry) state = state.copy(isLoading = true) },
                    )
                }
            }
        }
        composeRule.onNodeWithTag("today-retry").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("today-error").assertExists()
        composeRule.onNodeWithTag("today-retry").assertIsDisplayed().assertIsNotEnabled()

        state = state.copy(isLoading = false)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("today-retry").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun `first load still shows the skeleton`() {
        composeRule.setContent {
            MaterialTheme {
                TodayScreenContent(state = TodayUiState(isLoading = true), effects = emptyFlow(), onEvent = {})
            }
        }
        composeRule.onNodeWithTag("today-empty").assertDoesNotExist()
        composeRule.onNodeWithTag("today-error").assertDoesNotExist()
    }
}
