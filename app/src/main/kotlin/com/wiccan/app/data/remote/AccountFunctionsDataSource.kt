package com.wiccan.app.data.remote

import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper around the `deleteUserData` Cloud Function. The actual
 * profile, history, and Firebase Auth account deletion happen server
 * side under the Admin SDK; this call just triggers it for the caller.
 */
class AccountFunctionsDataSource @Inject constructor(
    private val functions: FirebaseFunctions,
) {
    suspend fun deleteUserData() {
        functions.getHttpsCallable(DELETE_FUNCTION_NAME).call().await()
    }

    suspend fun acceptTerms(termsVersion: Int, privacyVersion: Int) {
        functions.getHttpsCallable(ACCEPT_TERMS_FUNCTION_NAME)
            .call(mapOf("termsVersion" to termsVersion, "privacyVersion" to privacyVersion))
            .await()
    }

    private companion object {
        const val DELETE_FUNCTION_NAME = "deleteUserData"
        const val ACCEPT_TERMS_FUNCTION_NAME = "acceptTerms"
    }
}
