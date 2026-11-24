package com.jaimesorrow.wiccan.ui.today

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.ContentStatus
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.ContentType
import com.jaimesorrow.wiccan.ui.auth.SignInMode
import com.jaimesorrow.wiccan.ui.auth.SignInScreenContent
import com.jaimesorrow.wiccan.ui.auth.SignInUiState
import com.jaimesorrow.wiccan.ui.content.DailyGrimoirePage
import com.jaimesorrow.wiccan.ui.theme.LocallyResolvableTypography
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h760dp-xhdpi", application = android.app.Application::class)
class GrimoireScreenScreenshotTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `capture sign-up`() {
        composeRule.setContent {
            MaterialTheme(typography = LocallyResolvableTypography) {
                SignInScreenContent(
                    state = SignInUiState(mode = SignInMode.CREATE_ACCOUNT, username = "rowan"),
                    onEvent = {},
                    onGoogleSignInClick = {},
                    onOpenLegalDocument = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage(filePath = "app/build/outputs/roborazzi/wiccan-signup.png")
    }

    @Test
    fun `capture home`() {
        composeRule.setContent {
            MaterialTheme(typography = LocallyResolvableTypography) {
                TodayScreenContent(
                    state = TodayUiState(isLoading = false, lessonLocked = true),
                    effects = emptyFlow(),
                    onEvent = {},
                    onOpenToday = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage(filePath = "app/build/outputs/roborazzi/wiccan-home.png")
    }

    @Test
    fun `capture daily lesson`() {
        composeRule.setContent {
            MaterialTheme(typography = LocallyResolvableTypography) {
                DailyGrimoirePage(
                    content = Content(
                        id = "lesson-1",
                        type = ContentType.LESSON,
                        tier = ContentTier.PREMIUM,
                        tradition = "Contemporary Wicca",
                        category = ContentCategory.MOON_AND_SEASONAL,
                        title = "Ritual of the New Moon",
                        intro = "A quiet threshold for one intention.",
                        body = "Light one candle and name what you are willing to begin.",
                        materials = listOf("One candle", "A fireproof dish"),
                        safetyNote = "Never leave a flame unattended.",
                        tags = emptyList(),
                        status = ContentStatus.PUBLISHED,
                        reflection = "What did the dark make visible?",
                        practice = "Write the intention in your own words.",
                    ),
                )
            }
        }
        composeRule.onRoot().captureRoboImage(filePath = "app/build/outputs/roborazzi/wiccan-lesson.png")
    }
}
