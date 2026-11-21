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

/**
 * Registers a rotated FCM token with the server, and shows messages that
 * arrive while the app is in the foreground. (In the background the FCM SDK
 * draws `notification` messages in the tray itself and never calls
 * [onMessageReceived] for them.) Tapping either notification opens the
 * launcher activity, which lands on Today.
 */
@AndroidEntryPoint
class WiccanMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

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
        ) ?: return
        ForegroundNotificationPoster(applicationContext).post(content)
    }
}
