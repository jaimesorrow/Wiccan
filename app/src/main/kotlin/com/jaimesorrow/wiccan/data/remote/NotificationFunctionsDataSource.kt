package com.jaimesorrow.wiccan.data.remote

import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class NotificationFunctionsDataSource @Inject constructor(
    private val functions: FirebaseFunctions,
) {
    suspend fun registerToken(token: String, timeZone: String) {
        functions.getHttpsCallable(REGISTER_FUNCTION_NAME)
            .call(mapOf("token" to token, "timeZone" to timeZone))
            .await()
    }

    suspend fun unregisterToken(token: String) {
        functions.getHttpsCallable(UNREGISTER_FUNCTION_NAME).call(mapOf("token" to token)).await()
    }

    suspend fun setNotificationsEnabled(token: String, enabled: Boolean) {
        functions.getHttpsCallable(SET_NOTIFICATIONS_ENABLED_FUNCTION_NAME)
            .call(mapOf("token" to token, "enabled" to enabled))
            .await()
    }

    private companion object {
        const val REGISTER_FUNCTION_NAME = "registerDeviceToken"
        const val UNREGISTER_FUNCTION_NAME = "unregisterDeviceToken"
        const val SET_NOTIFICATIONS_ENABLED_FUNCTION_NAME = "setNotificationsEnabled"
    }
}
