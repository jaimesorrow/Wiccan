package com.wiccan.app.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.wiccan.app.domain.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Registers a rotated FCM token with the server. Foreground message
 * display (`onMessageReceived`) isn't implemented: a daily "your content
 * is ready" reminder has nothing useful to show a user already in the
 * app, and tapping the system-tray notification when backgrounded opens
 * the launcher activity (Today) by default — Wiccan has no other screen
 * yet to route to.
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
}
