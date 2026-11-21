package com.jaimesorrow.wiccan.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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

/** Separate id so a trial reminder never replaces (or is replaced by) the daily push. */
const val BILLING_REMINDER_NOTIFICATION_ID = 1002

/** Action + extra on the notification's tap intent asking MainActivity to show Today. */
const val ACTION_OPEN_TODAY = "com.jaimesorrow.wiccan.action.OPEN_TODAY"
const val EXTRA_OPEN_TODAY = "com.jaimesorrow.wiccan.extra.OPEN_TODAY"

/** Whether [intent] is a request (e.g. a notification tap) to navigate to Today. */
fun isOpenTodayRequest(intent: Intent?): Boolean = intent?.getBooleanExtra(EXTRA_OPEN_TODAY, false) == true

/** `data.type` the server sets on a trial-ending reminder (functions/src/trialEndingReminders.ts). */
const val BILLING_REMINDER_TYPE = "billing_reminder"

/** Google Play's subscription management page (all of this account's subscriptions). */
const val PLAY_SUBSCRIPTIONS_URL = "https://play.google.com/store/account/subscriptions"

/** Opens Google Play's subscription management, where the user can cancel or change a plan. */
fun manageSubscriptionIntent(): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_SUBSCRIPTIONS_URL))

/** What to show for an FCM message the app draws itself. */
data class ForegroundNotificationContent(
    val title: String?,
    val body: String?,
    /** The Android channel the server named; null means the daily-practice channel. */
    val channelId: String? = null,
    /** `data.type` from the server, e.g. [BILLING_REMINDER_TYPE]. */
    val type: String? = null,
    /** The account the server addressed; when set, only that signed-in account sees it. */
    val accountUid: String? = null,
) {
    val isBillingReminder: Boolean
        get() = channelId == BILLING_REMINDERS_CHANNEL_ID || type == BILLING_REMINDER_TYPE
}

/**
 * Picks the title/body from the FCM `notification` block, falling back to the
 * `data` payload's `title`/`body` (billing reminders are data-only messages).
 * Returns null when there is nothing to show.
 */
fun resolveForegroundContent(
    notificationTitle: String?,
    notificationBody: String?,
    data: Map<String, String>,
    channelId: String? = null,
): ForegroundNotificationContent? {
    val title = notificationTitle?.takeIf { it.isNotBlank() } ?: data["title"]?.takeIf { it.isNotBlank() }
    val body = notificationBody?.takeIf { it.isNotBlank() } ?: data["body"]?.takeIf { it.isNotBlank() }
    if (title == null && body == null) return null
    return ForegroundNotificationContent(
        title = title,
        body = body,
        channelId = channelId?.takeIf { it.isNotBlank() } ?: data["channelId"]?.takeIf { it.isNotBlank() },
        type = data["type"]?.takeIf { it.isNotBlank() },
        accountUid = data["uid"]?.takeIf { it.isNotBlank() },
    )
}

/**
 * Posts a notification the app draws itself: a daily-practice push received
 * in the foreground (in the background the FCM SDK draws those), or a
 * data-only trial/billing reminder in any app state. Daily pushes open
 * Today; billing reminders open Google Play's subscription management.
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
        val channelId = if (content.isBillingReminder) {
            ensureBillingRemindersChannel(context)
            BILLING_REMINDERS_CHANNEL_ID
        } else {
            ensureDailyPracticeChannel(context)
            DAILY_PRACTICE_CHANNEL_ID
        }
        val title = content.title ?: context.getString(
            if (content.isBillingReminder) R.string.notification_billing_default_title else R.string.notification_daily_default_title,
        )
        val tapIntent = if (content.isBillingReminder) {
            PendingIntent.getActivity(
                context,
                BILLING_REMINDER_NOTIFICATION_ID,
                manageSubscriptionIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        } else {
            PendingIntent.getActivity(
                context,
                DAILY_PRACTICE_NOTIFICATION_ID,
                // SINGLE_TOP reuses a running MainActivity (delivered to onNewIntent),
                // so the explicit extra is what moves it to Today from any screen.
                Intent(context, MainActivity::class.java)
                    .setAction(ACTION_OPEN_TODAY)
                    .putExtra(EXTRA_OPEN_TODAY, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_wiccan)
            .setContentTitle(title)
            .apply { content.body?.let { setContentText(it).setStyle(NotificationCompat.BigTextStyle().bigText(it)) } }
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(
                if (content.isBillingReminder) BILLING_REMINDER_NOTIFICATION_ID else DAILY_PRACTICE_NOTIFICATION_ID,
                notification,
            )
            true
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post.
            false
        }
    }
}
