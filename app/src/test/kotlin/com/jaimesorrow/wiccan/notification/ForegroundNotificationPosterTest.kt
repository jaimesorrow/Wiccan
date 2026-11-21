package com.jaimesorrow.wiccan.notification

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ForegroundNotificationPosterTest {

    private val context: Application = RuntimeEnvironment.getApplication()
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Test
    fun `foreground push is posted on the daily practice channel when permitted`() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val posted = ForegroundNotificationPoster(context).post(
            ForegroundNotificationContent(title = "Today's spell", body = "New Moon Intention Spell"),
        )

        assertTrue(posted)
        val notification = shadowOf(manager).getNotification(DAILY_PRACTICE_NOTIFICATION_ID)
        assertNotNull(notification)
        assertEquals(DAILY_PRACTICE_CHANNEL_ID, notification.channelId)
        assertEquals("Today's spell", shadowOf(notification).contentTitle)
        assertEquals("New Moon Intention Spell", shadowOf(notification).contentText)
        assertNotNull(notification.contentIntent)
        // The channel is (re)created if it doesn't exist yet.
        assertNotNull(manager.getNotificationChannel(DAILY_PRACTICE_CHANNEL_ID))
    }

    @Test
    fun `nothing is posted without POST_NOTIFICATIONS on API 33+`() {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val posted = ForegroundNotificationPoster(context).post(
            ForegroundNotificationContent(title = "Today's spell", body = "body"),
        )

        assertFalse(posted)
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }

    @Test
    fun `nothing is posted when the user turned notifications off`() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(manager).setNotificationsEnabled(false)

        assertFalse(ForegroundNotificationPoster(context).post(ForegroundNotificationContent("t", "b")))
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }

    @Test
    @Config(sdk = [30])
    fun `before API 33 no runtime permission is needed`() {
        assertTrue(ForegroundNotificationPoster(context).post(ForegroundNotificationContent(null, "body only")))
        val notification = shadowOf(manager).getNotification(DAILY_PRACTICE_NOTIFICATION_ID)
        assertEquals("Today's practice awaits", shadowOf(notification).contentTitle)
    }

    @Test
    fun `content comes from the notification block, falling back to data`() {
        assertEquals(
            ForegroundNotificationContent("Title", "Body"),
            resolveForegroundContent("Title", "Body", mapOf("title" to "ignored")),
        )
        assertEquals(
            ForegroundNotificationContent("Data title", "Data body"),
            resolveForegroundContent(null, " ", mapOf("title" to "Data title", "body" to "Data body")),
        )
        assertNull(resolveForegroundContent(null, null, emptyMap()))
        assertNull(resolveForegroundContent("", "", mapOf("unrelated" to "x")))
    }
}
