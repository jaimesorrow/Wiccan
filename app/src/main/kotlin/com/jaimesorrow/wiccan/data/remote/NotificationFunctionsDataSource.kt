package com.jaimesorrow.wiccan.data.remote

import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class NotificationFunctionsDataSource @Inject constructor(
    private val functions: FirebaseFunctions,
) {
    suspend fun registerToken(
        token: String,
        timeZone: String,
        uid: String,
        enabled: Boolean?,
        locale: String,
        updateChoice: Boolean = false,
    ) {
        val result = functions.getHttpsCallable(REGISTER_FUNCTION_NAME)
            .call(
                buildMap {
                    put("token", token)
                    put("timeZone", timeZone)
                    put("expectedUid", uid)
                    put("locale", locale)
                    enabled?.let { put("enabled", it) }
                    if (updateChoice) put("updateChoice", true)
                },
            )
            .await()
        check(!updateChoice || (result.data as? Map<*, *>)?.get("preferenceSupported") == true) {
            "The server did not confirm the notification preference."
        }
    }

    suspend fun unregisterToken(token: String, uid: String) {
        functions.getHttpsCallable(UNREGISTER_FUNCTION_NAME).call(mapOf("token" to token, "expectedUid" to uid)).await()
    }

    private companion object {
        const val REGISTER_FUNCTION_NAME = "registerDeviceToken"
        const val UNREGISTER_FUNCTION_NAME = "unregisterDeviceToken"
    }
}
