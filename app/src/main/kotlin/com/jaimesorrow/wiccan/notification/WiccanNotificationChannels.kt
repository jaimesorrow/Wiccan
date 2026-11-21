package com.jaimesorrow.wiccan.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.wiccan.app.R

/** Must match the channel id the daily FCM message names. */
const val DAILY_PRACTICE_CHANNEL_ID = "daily_practice"

fun ensureDailyPracticeChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    val channel = NotificationChannel(
        DAILY_PRACTICE_CHANNEL_ID,
        context.getString(R.string.notification_channel_daily_name),
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = context.getString(R.string.notification_channel_daily_description)
    }
    manager.createNotificationChannel(channel)
}

/** Must match BILLING_REMINDERS_CHANNEL_ID in functions/src/trialEndingReminders.ts. */
const val BILLING_REMINDERS_CHANNEL_ID = "billing_reminders"

/** A separate channel, so users can keep trial reminders while muting the daily push (or the reverse). */
fun ensureBillingRemindersChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    val channel = NotificationChannel(
        BILLING_REMINDERS_CHANNEL_ID,
        context.getString(R.string.notification_channel_billing_name),
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = context.getString(R.string.notification_channel_billing_description)
    }
    manager.createNotificationChannel(channel)
}
