package com.jaimesorrow.wiccan.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jaimesorrow.wiccan.MainActivity
import com.wiccan.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Stable id: a newer daily push replaces the previous one instead of stacking. */
const val DAILY_PRACTICE_NOTIFICATION_ID = 1001

/** Action + extra on the notification's tap intent asking MainActivity to show Today. */
const val ACTION_OPEN_TODAY = "com.jaimesorrow.wiccan.action.OPEN_TODAY"
const val EXTRA_OPEN_TODAY = "com.jaimesorrow.wiccan.extra.OPEN_TODAY"

/** Whether [intent] is a request (e.g. a notification tap) to navigate to Today. */
fun isOpenTodayRequest(intent: Intent?): Boolean = intent?.getBooleanExtra(EXTRA_OPEN_TODAY, false) == true

/** What to show for an FCM message received while the app is in the foreground. */
data class ForegroundNotificationContent(val title: String?, val body: String?)

/**
 * Picks the title/body from the FCM `notification` block, falling back to the
 * `data` payload's `title`/`body`. Returns null when there is nothing to show.
 */
fun resolveForegroundContent(
    notificationTitle: String?,
    notificationBody: String?,
    data: Map<String, String>,
): ForegroundNotificationContent? {
    val title = notificationTitle?.takeIf { it.isNotBlank() } ?: data["title"]?.takeIf { it.isNotBlank() }
    val body = notificationBody?.takeIf { it.isNotBlank() } ?: data["body"]?.takeIf { it.isNotBlank() }
    if (title == null && body == null) return null
    return ForegroundNotificationContent(title, body)
}

/**
 * Posts the daily-practice notification itself. When the app is backgrounded
 * the FCM SDK draws `notification` messages in the tray; in the foreground it
 * hands them to [WiccanMessagingService.onMessageReceived] instead and shows
 * nothing, so this mirrors the tray behaviour there.
 */
class ForegroundNotificationPoster @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /** Whether the user lets this app post notifications (POST_NOTIFICATIONS on API 33+). */
    fun canPost(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Returns true when a notification was posted. */
    fun post(content: ForegroundNotificationContent): Boolean {
        if (!canPost()) return false
        ensureDailyPracticeChannel(context)
        val title = content.title ?: context.getString(R.string.notification_daily_default_title)
        val openToday = PendingIntent.getActivity(
            context,
            0,
            // SINGLE_TOP reuses a running MainActivity (delivered to onNewIntent),
            // so the explicit extra is what moves it to Today from any screen.
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_OPEN_TODAY)
                .putExtra(EXTRA_OPEN_TODAY, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, DAILY_PRACTICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_wiccan)
            .setContentTitle(title)
            .apply { content.body?.let { setContentText(it).setStyle(NotificationCompat.BigTextStyle().bigText(it)) } }
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openToday)
            .setAutoCancel(true)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(DAILY_PRACTICE_NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post.
            false
        }
    }
}
