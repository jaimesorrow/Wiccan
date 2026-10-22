package com.jaimesorrow.wiccan.ui.content

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.ContentStatus
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.ContentType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ContentDetailViewTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `Today and Archive detail layout exposes all structured lesson sections`() {
        val content = Content(
            id = "lesson-1",
            type = ContentType.LESSON,
            tier = ContentTier.FREE,
            tradition = "Folk practice",
            category = ContentCategory.HOME_AND_HEARTH,
            title = "The Threshold",
            intro = "An entrance and a boundary.",
            body = "Notice what you carry into your home.",
            materials = null,
            safetyNote = null,
            tags = emptyList(),
            status = ContentStatus.PUBLISHED,
            reflection = "What should your home feel like?",
            practice = "Pause and choose one quality to bring inside.",
        )
        composeRule.setContent { MaterialTheme { ContentDetailView(content) } }
        for (text in listOf("Home & Hearth", "The Threshold", "Method", "Reflection", "Practice")) {
            composeRule.onNodeWithText(text).assertExists()
        }
        composeRule.onNodeWithText(content.reflection!!).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(content.practice!!).performScrollTo().assertIsDisplayed()
    }
}

