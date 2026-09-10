package com.nyxwell.app.data.remote

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import javax.inject.Inject

/**
 * Wraps Android Credential Manager's Google ID flow — the current
 * Google-recommended approach, superseding the older GoogleSignInClient.
 */
class GoogleCredentialDataSource @Inject constructor() {

    /** Returns the signed-in user's Google ID token, or throws if the flow fails or is cancelled. */
    suspend fun requestGoogleIdToken(activityContext: Context, webClientId: String): String {
        val credentialManager = CredentialManager.create(activityContext)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = credentialManager.getCredential(activityContext, request)
        val credential = response.credential

        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            error("Unexpected credential type from Credential Manager: ${credential.type}")
        }

        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
}
