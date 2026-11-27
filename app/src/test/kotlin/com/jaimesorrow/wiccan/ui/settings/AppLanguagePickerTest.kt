package com.jaimesorrow.wiccan.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppLanguagePickerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `picker shows all supported choices and reports a selection`() {
        var selected = AppLanguage.SYSTEM
        composeRule.setContent {
            MaterialTheme {
                AppLanguagePicker(selected = selected, onSelected = { selected = it })
            }
        }
        composeRule.onNodeWithText("Use device language").assertIsDisplayed()
        composeRule.onNodeWithText("Español").assertIsDisplayed()
        composeRule.onNodeWithText("Português (Brasil)").assertIsDisplayed().performClick()
        assertEquals(AppLanguage.PORTUGUESE_BRAZIL, selected)
    }
}
