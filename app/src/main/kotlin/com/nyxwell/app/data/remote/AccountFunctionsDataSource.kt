package com.nyxwell.app.data.remote

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
        functions.getHttpsCallable(FUNCTION_NAME).call().await()
    }

    private companion object {
        const val FUNCTION_NAME = "deleteUserData"
    }
}
