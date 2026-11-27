package com.jaimesorrow.wiccan.ui.notification

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jaimesorrow.wiccan.domain.repository.NotificationRepository
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The post-purchase explanation is shown once per composition when the
 * Android 13+ permission is missing. "Not now" dismisses it; a grant skips
 * the dialog and registers the token. There is no persistent shown-once
 * flag across process restarts.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class NotificationSetupEffectTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `explains the permission once and hides it after not now`() {
        shadowOf(composeRule.activity).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val viewModel = NotificationSetupViewModel(mockk(relaxed = true))
        composeRule.setContent {
            MaterialTheme { NotificationSetupEffect(viewModel) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Let the grimoire call to you each dawn").assertIsDisplayed()
        composeRule.onNodeWithText("Not now").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Let the grimoire call to you each dawn").assertDoesNotExist()
    }

    @Test
    fun `already granted permission skips the explanation and registers the token`() {
        shadowOf(composeRule.activity).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val notifications = mockk<NotificationRepository>(relaxed = true)
        val viewModel = NotificationSetupViewModel(notifications)
        composeRule.setContent {
            MaterialTheme { NotificationSetupEffect(viewModel) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Let the grimoire call to you each dawn").assertDoesNotExist()
        coVerify(timeout = 2_000, exactly = 1) { notifications.refreshAndRegisterToken() }
    }
}
