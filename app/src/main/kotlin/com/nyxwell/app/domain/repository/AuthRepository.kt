package com.nyxwell.app.domain.repository

import android.content.Context
import com.nyxwell.app.core.result.AppResult
import com.nyxwell.app.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

/**
 * The only layer allowed to touch FirebaseAuth or Credential Manager
 * directly. `users/{uid}` creation itself happens server-side (an Auth
 * trigger), never here — this repository only establishes the session.
 */
interface AuthRepository {
    fun observeAuthState(): Flow<AuthState>

    /**
     * [context] must be an Activity context — Credential Manager attaches
     * its account-picker UI to it, the same requirement Billing has for
     * launching its purchase flow.
     */
    suspend fun signInWithGoogle(context: Context): AppResult<Unit>

    suspend fun signInWithEmail(email: String, password: String): AppResult<Unit>

    suspend fun createAccountWithEmail(email: String, password: String): AppResult<Unit>

    suspend fun sendPasswordReset(email: String): AppResult<Unit>

    suspend fun signOut(): AppResult<Unit>

    /**
     * Deletes the signed-in user's server-side data (profile, delivery
     * history) and their Firebase Auth account. Does not sign out locally —
     * callers that need the root observer to route back to sign-in should
     * follow a successful delete with [signOut].
     */
    suspend fun deleteAccount(): AppResult<Unit>
}
