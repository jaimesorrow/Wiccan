package com.wiccan.app.domain.repository

import android.content.Context
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

/**
 * The only layer allowed to touch FirebaseAuth or Credential Manager
 * directly. `users/{uid}` creation itself happens server-side (an Auth
 * trigger), never here — this repository only establishes the session.
 */
interface AuthRepository {
    fun observeAuthState(): Flow<AuthState>

    /**
     * Whether the server-side `users/{uid}` profile document exists yet.
     * Its creation is a Firebase Auth trigger that runs asynchronously
     * after sign-up, so a client can briefly observe [AuthState.SignedIn]
     * before this turns true - the root state machine uses it to hold on
     * an "initializing" state rather than entering the app against a
     * profile that isn't there yet.
     */
    fun observeProfileReady(uid: String): Flow<Boolean>

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
