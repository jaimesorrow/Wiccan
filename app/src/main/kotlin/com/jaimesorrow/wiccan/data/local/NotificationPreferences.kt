package com.jaimesorrow.wiccan.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_preferences")

/**
 * Local cache of the notifications-enabled toggle, purely so Settings can
 * show the right switch state across app restarts. The server-side
 * users/{uid}/devices doc is the real source of truth for whether a push
 * actually gets sent, but Firestore rules give the client no read access
 * to it (server-only - see firestore.rules), so there's nothing to read
 * back from there; this local copy is what the UI reflects instead.
 */
@Singleton
class NotificationPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun observe(uid: String): Flow<Boolean> = context.notificationDataStore.data.map {
        it[accountKey(uid)] ?: true
    }

    suspend fun savedChoice(uid: String): Boolean? {
        val key = accountKey(uid)
        return context.notificationDataStore.edit { values ->
            values.remove(LEGACY_KEY)?.let { previous ->
                if (values[key] == null) values[key] = previous
            }
        }[key]
    }

    suspend fun setNotificationsEnabled(uid: String, enabled: Boolean) {
        context.notificationDataStore.edit {
            it.remove(LEGACY_KEY)
            it[accountKey(uid)] = enabled
        }
    }

    private companion object {
        val LEGACY_KEY = booleanPreferencesKey("notifications_enabled")
        fun accountKey(uid: String) = booleanPreferencesKey("notifications_enabled_$uid")
    }
}
