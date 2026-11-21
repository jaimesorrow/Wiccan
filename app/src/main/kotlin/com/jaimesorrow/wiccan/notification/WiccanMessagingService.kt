package com.jaimesorrow.wiccan.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jaimesorrow.wiccan.domain.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Registers a rotated FCM token with the server, and shows the messages the
 * app draws itself: daily `notification` messages that arrive while the app
 * is in the foreground (in the background the FCM SDK draws those and never
 * calls [onMessageReceived]), and data-only trial/billing reminders in any
 * app state. Nothing is shown after sign-out; see [ForegroundPushHandler].
 * A daily push opens Today; a billing reminder opens Play subscriptions.
 */
@AndroidEntryPoint
class WiccanMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var foregroundPushHandler: ForegroundPushHandler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch { notificationRepository.registerToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val content = resolveForegroundContent(
            notificationTitle = message.notification?.title,
            notificationBody = message.notification?.body,
            data = message.data,
            channelId = message.notification?.channelId,
        ) ?: return
        // Runs on FCM's worker thread; the handler only reads local state.
        runBlocking { foregroundPushHandler.handle(content) }
    }
}
