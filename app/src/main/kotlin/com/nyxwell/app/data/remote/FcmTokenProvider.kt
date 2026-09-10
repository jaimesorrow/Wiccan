package com.nyxwell.app.data.remote

import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class FcmTokenProvider @Inject constructor(
    private val messaging: FirebaseMessaging,
) {
    suspend fun getToken(): String = messaging.token.await()
}
