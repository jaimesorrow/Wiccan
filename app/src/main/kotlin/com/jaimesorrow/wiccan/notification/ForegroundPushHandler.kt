package com.jaimesorrow.wiccan.notification

import com.jaimesorrow.wiccan.core.auth.CurrentUserProvider
import com.jaimesorrow.wiccan.domain.repository.NotificationRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

/** How long to wait for the stored reminder choice before giving up (and not posting). */
private const val PREFERENCE_READ_TIMEOUT_MS = 2_000L

/**
 * Decides whether a push the app draws itself should be shown. A push can be
 * in flight when the user signs out or turns the daily reminder off, so
 * nothing is posted unless someone is signed in, and a push addressed to an
 * account (`data.uid`) is shown only to that account.
 *
 * - Daily practice: also needs this account's daily reminder on
 *   ([NotificationRepository.notificationsEnabled] is false when signed out).
 * - Trial/billing reminders: do not depend on the daily switch; the user
 *   mutes them with the separate `billing_reminders` Android channel, which
 *   Android applies when the notification is posted.
 */
class ForegroundPushHandler @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val poster: ForegroundNotificationPoster,
) {
    /** Returns true when a notification was posted. */
    suspend fun handle(content: ForegroundNotificationContent): Boolean {
        if (!addressedToSignedInAccount(content)) return false
        if (content.isBillingReminder) return poster.post(content)
        val enabled = withTimeoutOrNull(PREFERENCE_READ_TIMEOUT_MS) {
            notificationRepository.notificationsEnabled
                .catch { t -> if (t is CancellationException) throw t else emit(false) }
                .firstOrNull()
        } ?: false
        if (!enabled || !addressedToSignedInAccount(content)) return false
        return poster.post(content)
    }

    private fun addressedToSignedInAccount(content: ForegroundNotificationContent): Boolean {
        val uid = currentUserProvider.currentUserId ?: return false
        return content.accountUid == null || content.accountUid == uid
    }
}
