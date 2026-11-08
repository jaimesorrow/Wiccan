package com.jaimesorrow.wiccan.ui.settings

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.jaimesorrow.wiccan.analytics.AnalyticsConsentManager
import com.jaimesorrow.wiccan.core.config.PolicyUrls
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.data.local.ThemePreferences
import com.jaimesorrow.wiccan.domain.model.WiccanThemeMode
import com.jaimesorrow.wiccan.domain.repository.NotificationRepository
import com.jaimesorrow.wiccan.domain.usecase.DeleteAccountUseCase
import com.jaimesorrow.wiccan.ui.auth.FakeAuthRepository
import com.jaimesorrow.wiccan.ui.paywall.FakeSubscriptionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Clicks every Settings button against the real ViewModel backed by fakes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class SettingsScreenButtonsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var auth: FakeAuthRepository
    private lateinit var notifications: NotificationRepository
    private lateinit var consent: AnalyticsConsentManager
    private val consentFlow = MutableStateFlow(false)
    private val clicks = mutableListOf<String>()

    @Before
    fun setUp() {
        auth = FakeAuthRepository()
        notifications = mockk(relaxed = true)
        every { notifications.notificationsEnabled } returns flowOf(true)
        coEvery { notifications.setNotificationsEnabled(any()) } returns AppResult.Success(Unit)
        coEvery { notifications.unregisterToken() } returns AppResult.Success(Unit)
        consent = mockk(relaxed = true)
        every { consent.consent } returns consentFlow
        coEvery { consent.setConsent(any()) } answers { consentFlow.value = firstArg() }
    }

    private fun launch(accountOnly: Boolean = false) {
        val themePreferences: ThemePreferences = mockk(relaxed = true)
        every { themePreferences.themeMode } returns flowOf(WiccanThemeMode.DEFAULT)
        val viewModel = SettingsViewModel(
            auth,
            DeleteAccountUseCase(auth, mockk(relaxed = true)),
            FakeSubscriptionRepository(),
            notifications,
            themePreferences,
            mockk(relaxed = true),
            consent,
        )
        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    accountOnly = accountOnly,
                    onBack = { clicks += "back" },
                    onTermsClick = { clicks += "terms" },
                    onPrivacyClick = { clicks += "privacy" },
                    onContentSafetyClick = { clicks += "safety" },
                )
            }
        }
    }

    private fun click(text: String) {
        composeRule.onNodeWithText(text).performScrollTo().performClick()
        composeRule.waitForIdle()
    }

    private fun lastStartedIntent(): Intent = shadowOf(composeRule.activity).nextStartedActivity

    @Test
    fun `legal links call their navigation callbacks`() {
        launch()
        click("Terms of Service")
        click("Privacy Policy")
        click("Content & Safety")
        assertEquals(listOf("terms", "privacy", "safety"), clicks)
    }

    @Test
    fun `Contact Support opens an email to the support address`() {
        launch()
        click("Contact Support")
        val intent = lastStartedIntent()
        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("mailto:${PolicyUrls.SUPPORT_EMAIL}", intent.data.toString())
    }

    @Test
    fun `Manage Subscription opens Google Play subscriptions`() {
        launch()
        click("Manage Subscription")
        val intent = lastStartedIntent()
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://play.google.com/store/account/subscriptions", intent.data.toString())
    }

    @Test
    fun `Sign Out signs the user out`() {
        launch()
        click("Sign Out")
        assertEquals(1, auth.signOutCallCount)
    }

    @Test
    fun `Delete Account asks for confirmation, Cancel keeps the account, Delete removes it`() {
        launch()
        click("Delete Account")
        composeRule.onNodeWithText("Delete account?").assertExists()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Delete account?").assertDoesNotExist()
        assertEquals(0, auth.deleteAccountCallCount)

        click("Delete Account")
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitForIdle()
        assertEquals(1, auth.deleteAccountCallCount)
    }

    @Test
    fun `notifications switch updates the server preference`() {
        launch()
        composeRule.onNodeWithText("Daily Notifications").performScrollTo()
        val switch = composeRule.onAllNodes(isToggleable())[0]
        switch.assertIsOn().performClick()
        composeRule.waitForIdle()
        coVerify { notifications.setNotificationsEnabled(false) }
    }

    @Test
    fun `analytics switch is off by default and records consent`() {
        launch()
        composeRule.onNodeWithText("Usage analytics & crash reports").performScrollTo()
        val switch = composeRule.onAllNodes(isToggleable())[1]
        switch.assertIsOff().performClick()
        composeRule.waitForIdle()
        coVerify { consent.setConsent(true) }
        switch.assertIsOn()
    }

    @Test
    fun `account-only mode shows Back, hides app preferences, keeps privacy controls`() {
        launch(accountOnly = true)
        click("← Back")
        assertEquals(listOf("back"), clicks)
        composeRule.onNodeWithText("Daily Notifications").assertDoesNotExist()
        composeRule.onNodeWithText("Theme").assertDoesNotExist()
        composeRule.onNodeWithText("Usage analytics & crash reports").assertExists()
        composeRule.onNodeWithText("Delete Account").assertExists()
    }
}
