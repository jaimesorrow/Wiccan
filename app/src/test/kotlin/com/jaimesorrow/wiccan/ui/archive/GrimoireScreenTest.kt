package com.jaimesorrow.wiccan.ui.archive

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        composeRule.setContent {
            MaterialTheme { ArchiveScreenContent(ArchiveUiState(isLoading = false), onEvent = {}) }
        }
        composeRule.onNodeWithText("Grimoire").assertIsDisplayed()
        composeRule.onNodeWithText("Your Grimoire is empty. Open Today to receive your first lesson.").assertIsDisplayed()
        composeRule.onNodeWithText("Library").assertDoesNotExist()
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
        composeRule.onNodeWithText("Protection (1)").assertIsDisplayed()
        composeRule.onNodeWithText(entry.title).assertIsDisplayed().performClick()
        assertEquals(ArchiveUiEvent.EntryClicked("old-lesson"), event)
    }
}
