package com.wiccan.app.domain.repository

import android.content.Context
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.domain.model.AccountStatus
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
     * Where the server-side `users/{uid}` profile stands for [uid]:
     * not yet created by the Auth trigger, created but missing current
     * policy acceptance, or fully ready. The root state machine uses
     * this to hold on an intermediate state rather than entering the
     * app against a profile that isn't there yet or hasn't accepted the
     * current Terms/Privacy Policy.
     */
    fun observeAccountStatus(uid: String): Flow<AccountStatus>

    /**
     * Records acceptance of the current Terms of Service/Privacy Policy
     * ([com.wiccan.app.domain.model.PolicyVersions]) for the signed-in
     * user. The server rejects anything other than its own current
     * versions, so this always sends the client's current constants.
     */
    suspend fun acceptTerms(): AppResult<Unit>

    /** Whether this build has the web OAuth client required for Google sign-in. */
    fun isGoogleSignInAvailable(): Boolean

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
