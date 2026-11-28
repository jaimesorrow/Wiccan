package com.jaimesorrow.wiccan.ui.archive

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.ContentType
import com.jaimesorrow.wiccan.domain.model.HistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class GrimoireScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `empty collection tells the user delivered lessons are in Grimoire`() {
        var selectedTab by mutableStateOf(ArchiveTab.TIMELINE)
        composeRule.setContent {
            MaterialTheme {
                ArchiveScreenContent(
                    ArchiveUiState(isLoading = false, selectedTab = selectedTab),
                    onEvent = { event ->
                        if (event is ArchiveUiEvent.TabSelected) {
                            selectedTab = event.tab
                        }
                    },
                )
            }
        }
        composeRule.onNodeWithTag("grimoire-logo").assertIsDisplayed()
        composeRule.onNodeWithText("Grimoire").assertIsDisplayed()
        composeRule
            .onNodeWithText("Your Grimoire is still quiet. Open Today to receive your first lesson.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("What the days have already given you.").assertIsDisplayed()
        composeRule.onNodeWithText("Categories").assertIsDisplayed()

        composeRule.onNodeWithText("Categories").performClick()
        composeRule.onNodeWithText("Candle Spells (0)").performScrollTo().assertIsDisplayed()
        assertEquals(16, ContentCategory.libraryCategories.size)
        assertEquals(ContentCategory.PROTECTION_CLEANSING_WARDING, ContentCategory.libraryCategories.last())
    }

    @Test
    fun `historical delivered lesson remains grouped and opens the same detail route`() {
        val entry = HistoryEntry(
            contentId = "old-lesson",
            title = "A lesson from last year",
            type = ContentType.LESSON,
            tradition = "Contemporary Wicca",
            category = ContentCategory.PROTECTION,
            deliveredDate = "2025-01-01",
        )
        var event: ArchiveUiEvent? = null
        composeRule.setContent {
            MaterialTheme {
                ArchiveScreenContent(
                    ArchiveUiState(isLoading = false, entries = listOf(entry), selectedTab = ArchiveTab.LIBRARY),
                    onEvent = { event = it },
                )
            }
        }
        composeRule.onNodeWithText("Protection (1)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(entry.title).performScrollTo().assertIsDisplayed().performClick()
        assertEquals(ArchiveUiEvent.EntryClicked("old-lesson"), event)
    }

    @Test
    fun `search reports no matching entries`() {
        val entry = HistoryEntry(
            contentId = "entry-1",
            title = "Candle of Clear Intent",
            type = ContentType.SPELL,
            tradition = "European folk magic",
            category = ContentCategory.CANDLE_SPELLS,
            deliveredDate = "2026-11-27",
        )
        var state by mutableStateOf(ArchiveUiState(isLoading = false, entries = listOf(entry)))
        composeRule.setContent {
            MaterialTheme {
                ArchiveScreenContent(
                    state = state,
                    onEvent = { event ->
                        state = when (event) {
                            is ArchiveUiEvent.SearchQueryChanged -> state.copy(searchQuery = event.query)
                            ArchiveUiEvent.SearchCleared -> state.copy(searchQuery = "")
                            else -> state
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithTag("grimoire-search").performTextInput("missing")
        composeRule.onNodeWithText("No entries match your search.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Clear").assertIsDisplayed().performClick()
        composeRule.onNodeWithText(entry.title).performScrollTo().assertIsDisplayed()
    }
}
