package com.jaimesorrow.wiccan.ui.today

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.jaimesorrow.wiccan.AccountStatusError
import com.jaimesorrow.wiccan.LoadingIndicator
import com.jaimesorrow.wiccan.analytics.AnalyticsConsentManager
import com.jaimesorrow.wiccan.analytics.AnalyticsLogger
import com.jaimesorrow.wiccan.data.local.ThemePreferences
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.ContentStatus
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.ContentType
import com.jaimesorrow.wiccan.domain.model.HistoryEntry
import com.jaimesorrow.wiccan.domain.model.WiccanThemeMode
import com.jaimesorrow.wiccan.domain.repository.NotificationRepository
import com.jaimesorrow.wiccan.domain.usecase.DeleteAccountUseCase
import com.jaimesorrow.wiccan.ui.archive.ArchiveScreenContent
import com.jaimesorrow.wiccan.ui.archive.ArchiveTab
import com.jaimesorrow.wiccan.ui.archive.ArchiveUiState
import com.jaimesorrow.wiccan.ui.auth.FakeAuthRepository
import com.jaimesorrow.wiccan.ui.auth.OnboardingScreen
import com.jaimesorrow.wiccan.ui.auth.OnboardingViewModel
import com.jaimesorrow.wiccan.ui.auth.SignInMode
import com.jaimesorrow.wiccan.ui.auth.SignInScreenContent
import com.jaimesorrow.wiccan.ui.auth.SignInUiState
import com.jaimesorrow.wiccan.ui.brand.WiccanLineArt
import com.jaimesorrow.wiccan.ui.brand.WiccanLineArtKind
import com.jaimesorrow.wiccan.ui.content.ContentDetailView
import com.jaimesorrow.wiccan.ui.content.DailyGrimoirePage
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentId
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentScreen
import com.jaimesorrow.wiccan.ui.legal.WiccanLegalDocuments
import com.jaimesorrow.wiccan.ui.navigation.WiccanBottomBarContent
import com.jaimesorrow.wiccan.ui.navigation.WiccanDestinations
import com.jaimesorrow.wiccan.ui.paywall.FakeSubscriptionRepository
import com.jaimesorrow.wiccan.ui.paywall.PaywallScreen
import com.jaimesorrow.wiccan.ui.paywall.PaywallViewModel
import com.jaimesorrow.wiccan.ui.policy.PolicyAcceptanceScreen
import com.jaimesorrow.wiccan.ui.policy.PolicyAcceptanceViewModel
import com.jaimesorrow.wiccan.ui.settings.SettingsScreen
import com.jaimesorrow.wiccan.ui.settings.SettingsViewModel
import com.jaimesorrow.wiccan.ui.theme.MidnightGrimoireTheme
import com.jaimesorrow.wiccan.widget.WiccanWidgetPreview
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
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
    @Config(sdk = [34], qualifiers = "w360dp-h1600dp-xhdpi", application = android.app.Application::class)
    fun `capture sign-up`() = capture("wiccan-signup") {
        SignInScreenContent(
            state = SignInUiState(mode = SignInMode.CREATE_ACCOUNT, username = "rowan"),
            onEvent = {},
            onGoogleSignInClick = {},
            onOpenLegalDocument = {},
        )
    }

    @Test
    fun `capture onboarding`() {
        val viewModel = OnboardingViewModel(FakeAuthRepository())
        capture("wiccan-onboarding") {
            OnboardingScreen(verification = false, viewModel = viewModel)
        }
    }

    @Test
    fun `capture policy`() {
        val consent = mockk<AnalyticsConsentManager>(relaxed = true)
        every { consent.consent } returns flowOf(false)
        val viewModel = PolicyAcceptanceViewModel(FakeAuthRepository(), consent)
        capture("wiccan-policy") {
            PolicyAcceptanceScreen(viewModel = viewModel)
        }
    }

    @Test
    fun `capture paywall`() {
        val viewModel = PaywallViewModel(FakeSubscriptionRepository(), mockk<AnalyticsLogger>(relaxed = true))
        capture("wiccan-paywall") {
            MidnightGrimoireTheme { PaywallScreen(viewModel = viewModel) }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("7 days free").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onRoot().captureRoboImage(filePath = "build/outputs/roborazzi/wiccan-paywall.png")
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h2200dp-xhdpi", application = android.app.Application::class)
    fun `capture settings`() {
        val viewModel = settingsViewModel()
        capture("wiccan-settings") {
            MidnightGrimoireTheme { SettingsScreen(viewModel = viewModel) }
        }
    }

    @Test
    fun `capture settings delete`() {
        val viewModel = settingsViewModel()
        composeRule.setContent { SettingsScreen(viewModel = viewModel) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Delete Account").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(filePath = "build/outputs/roborazzi/wiccan-settings-delete.png")
    }

    @Test
    fun `capture legal`() = capture("wiccan-legal") {
        LegalDocumentScreen(document = WiccanLegalDocuments.get(LegalDocumentId.TERMS), onBack = {})
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h2400dp-xhdpi", application = android.app.Application::class)
    fun `capture home`() = capture("wiccan-home") {
        TodayScreenContent(
            state = TodayUiState(isLoading = false, lessonLocked = true),
            effects = emptyFlow(),
            onEvent = {},
            onOpenToday = {},
        )
    }

    @Test
    fun `capture today empty`() = capture("wiccan-today-empty") {
        TodayScreenContent(
            state = TodayUiState(isLoading = false, poolExhausted = true),
            effects = emptyFlow(),
            onEvent = {},
        )
    }

    @Test
    fun `capture today error`() = capture("wiccan-today-error") {
        TodayScreenContent(
            state = TodayUiState(isLoading = false, loadFailed = true),
            effects = emptyFlow(),
            onEvent = {},
        )
    }

    @Test
    fun `capture daily lesson`() {
        composeRule.setContent {
            MidnightGrimoireTheme {
                DailyGrimoirePage(content = sampleLesson())
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(filePath = "build/outputs/roborazzi/wiccan-lesson.png")
    }

    @Test
    fun `capture daily lesson middle`() {
        composeRule.setContent {
            MidnightGrimoireTheme {
                DailyGrimoirePage(content = sampleLesson())
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lesson_steps_parchment").performScrollTo()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(filePath = "build/outputs/roborazzi/wiccan-lesson-middle.png")
    }

    @Test
    fun `capture daily lesson bottom`() {
        composeRule.setContent {
            MidnightGrimoireTheme {
                DailyGrimoirePage(content = sampleLesson())
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lesson_quill_ornament").performScrollTo()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(filePath = "build/outputs/roborazzi/wiccan-lesson-bottom.png")
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h640dp-xhdpi", application = android.app.Application::class)
    fun `capture daily lesson with reflection below parchment`() {
        composeRule.setContent {
            MidnightGrimoireTheme {
                DailyGrimoirePage(content = sampleLesson())
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lesson_reflection_shell").performScrollTo()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/wiccan-lesson-working-and-reflection.png",
        )
    }

    @Test
    fun `capture daily lesson at 130 percent text`() {
        composeRule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 1.3f)) {
                MidnightGrimoireTheme {
                    DailyGrimoirePage(content = sampleLesson())
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lesson_steps_parchment").performScrollTo()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/wiccan-lesson-font-130-parchment.png",
        )
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h2800dp-xhdpi", application = android.app.Application::class)
    fun `capture home cosmic at 130 percent text`() {
        composeRule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 1.3f)) {
                MidnightGrimoireTheme {
                    SpellbookHome(
                        content = sampleLesson(),
                        lessonLocked = false,
                        onOpenToday = {},
                        onOpenPaywall = {},
                        onOpenGrimoire = {},
                        onOpenSettings = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_cosmic_card").performScrollTo()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(filePath = "build/outputs/roborazzi/wiccan-home-cosmic-130.png")
    }

    @Test
    fun `capture settings manage subscription`() {
        val viewModel = settingsViewModel()
        composeRule.setContent {
            MidnightGrimoireTheme { SettingsScreen(viewModel = viewModel) }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings-manage-subscription").performScrollTo()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/wiccan-settings-manage-subscription.png",
        )
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h2400dp-xhdpi", application = android.app.Application::class)
    fun `capture line art contact sheet`() {
        composeRule.setContent {
            MidnightGrimoireTheme {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 5,
                    ) {
                        WiccanLineArtKind.entries.forEach { kind ->
                            WiccanLineArt(kind = kind, modifier = Modifier.size(48.dp))
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(filePath = "build/outputs/roborazzi/wiccan-line-art-contact-sheet.png")
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h2400dp-xhdpi", application = android.app.Application::class)
    fun `capture daily lesson full content`() {
        composeRule.setContent {
            MidnightGrimoireTheme {
                DailyGrimoirePage(
                    content = sampleLongLesson(),
                    fillViewport = false,
                    modifier = Modifier.testTag("grimoire_daily_lesson"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("grimoire_daily_lesson").captureRoboImage(
            filePath = "build/outputs/roborazzi/wiccan-lesson-full.png",
        )
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h1600dp-xhdpi", application = android.app.Application::class)
    fun `capture archive`() = capture("wiccan-archive") {
        ArchiveScreenContent(
            state = ArchiveUiState(
                isLoading = false,
                entries = listOf(
                    HistoryEntry(
                        contentId = "lesson-1",
                        title = "Ritual of the New Moon",
                        type = ContentType.LESSON,
                        tradition = "Contemporary Wicca",
                        category = ContentCategory.MOON_AND_SEASONAL,
                        deliveredDate = "2026-11-24",
                    ),
                ),
            ),
            onEvent = {},
        )
    }

    @Test
    fun `capture archive detail`() = capture("wiccan-archive-detail") {
        MidnightGrimoireTheme { ContentDetailView(content = sampleLesson()) }
    }

    @Test
    fun `capture bottom bar`() = capture("wiccan-nav") {
        MidnightGrimoireTheme {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    WiccanBottomBarContent(currentRoute = WiccanDestinations.HOME, onNavigate = {})
                },
            ) { insets ->
                Column(Modifier.padding(insets).fillMaxSize().padding(24.dp)) {
                    Text(
                        text = "Home",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }
        }
    }

    @Test
    fun `capture loading`() = capture("wiccan-loading") {
        MidnightGrimoireTheme { LoadingIndicator() }
    }

    @Test
    fun `capture account error`() = capture("wiccan-account-error") {
        MidnightGrimoireTheme {
            AccountStatusError(message = "The working stumbled. Try again in a moment.", onRetry = {})
        }
    }

    @Test
    fun `capture widget`() = capture("wiccan-widget") {
        WiccanWidgetPreview()
    }

    private fun capture(name: String, content: @Composable () -> Unit) {
        composeRule.setContent { content() }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(filePath = "build/outputs/roborazzi/$name.png")
    }

    private fun settingsViewModel(): SettingsViewModel {
        val notifications = mockk<NotificationRepository>(relaxed = true)
        every { notifications.notificationsEnabled } returns flowOf(true)
        val themePreferences = mockk<ThemePreferences>(relaxed = true)
        every { themePreferences.themeMode } returns flowOf(WiccanThemeMode.DEFAULT)
        val consent = mockk<AnalyticsConsentManager>(relaxed = true)
        every { consent.consent } returns flowOf(false)
        return SettingsViewModel(
            FakeAuthRepository(),
            DeleteAccountUseCase(FakeAuthRepository(), mockk(relaxed = true)),
            FakeSubscriptionRepository(),
            notifications,
            themePreferences,
            mockk(relaxed = true),
            consent,
        )
    }

    private fun sampleLongLesson(): Content {
        val body = buildString {
            repeat(22) {
                appendLine("Breathe with the flame and name one honest intention you can tend this month.")
            }
        }
        return sampleLesson().copy(body = body)
    }

    private fun sampleLesson() = Content(
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
    )
}
