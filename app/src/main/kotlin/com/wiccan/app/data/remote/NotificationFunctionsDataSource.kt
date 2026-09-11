package com.wiccan.app.data.remote

import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class NotificationFunctionsDataSource @Inject constructor(
    private val functions: FirebaseFunctions,
) {
    suspend fun registerToken(token: String) {
        functions.getHttpsCallable(FUNCTION_NAME).call(mapOf("token" to token)).await()
    }

    private companion object {
        const val FUNCTION_NAME = "registerFcmToken"
    }
}
