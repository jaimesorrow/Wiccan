package com.jaimesorrow.wiccan.billing

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class PlayPromoCodeRedeemTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `format trims and uppercases`() {
        assertEquals("WICCANLAUNCH2026", formatPromoCodeForPlay("  wiccanlaunch2026 "))
    }

    @Test
    fun `redeem uri URL-encodes special characters`() {
        assertEquals(
            "https://play.google.com/redeem?code=SAVE%2B20",
            playPromoRedeemUri(" save+20 ").toString(),
        )
    }

    @Test
    fun `launch prefers Play Store and falls back to browser`() {
        launchPlayPromoRedeem(composeRule.activity, "WICCANLAUNCH2026")
        val intent = shadowOf(composeRule.activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(
            "https://play.google.com/redeem?code=WICCANLAUNCH2026",
            intent.data.toString(),
        )
        assertNull(intent.`package`)
    }
}
