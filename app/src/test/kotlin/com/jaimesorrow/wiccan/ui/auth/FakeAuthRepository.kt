package com.jaimesorrow.wiccan.ui.auth

import android.content.Context
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.domain.model.AccountStatus
import com.jaimesorrow.wiccan.domain.model.AuthState
import com.jaimesorrow.wiccan.domain.model.ProfileName
import com.jaimesorrow.wiccan.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository : AuthRepository {

    private val authStateFlow = MutableStateFlow<AuthState>(AuthState.SignedOut)
    private val accountStatusFlows = mutableMapOf<String, MutableStateFlow<AccountStatus>>()

    var googleSignInAvailable: Boolean = true
    var googleResult: AppResult<Unit> = AppResult.Success(Unit)
    var emailSignInResult: AppResult<Unit> = AppResult.Success(Unit)
    var createAccountResult: AppResult<Unit> = AppResult.Success(Unit)
    var passwordResetResult: AppResult<Unit> = AppResult.Success(Unit)
    var deleteAccountResult: AppResult<Unit> = AppResult.Success(Unit)
    var acceptTermsResult: AppResult<Unit> = AppResult.Success(Unit)
    var verificationDeliveryFailurePending: Boolean = false

    var lastSignInEmail: String? = null
    var lastSignInPassword: String? = null
    var lastCreateAccountEmail: String? = null
    var lastPasswordResetEmail: String? = null
    var signOutCallCount: Int = 0
    var deleteAccountCallCount: Int = 0
    var acceptTermsCallCount: Int = 0
    var lastCompletedUsername: String? = null

    override fun observeAuthState(): Flow<AuthState> = authStateFlow

    /** Defaults to "ready" so tests unrelated to profile initialization don't need to opt in. */
    override fun observeAccountStatus(uid: String): Flow<AccountStatus> =
        accountStatusFlows.getOrPut(uid) { MutableStateFlow(AccountStatus.Ready) }

    fun setAccountStatus(uid: String, status: AccountStatus) {
        accountStatusFlows.getOrPut(uid) { MutableStateFlow(status) }.value = status
    }

    fun setAuthState(state: AuthState) {
        authStateFlow.value = state
    }

    override suspend fun acceptTerms(): AppResult<Unit> {
        acceptTermsCallCount++
        return acceptTermsResult
    }

    override fun isGoogleSignInAvailable(): Boolean = googleSignInAvailable

    override suspend fun signInWithGoogle(context: Context): AppResult<Unit> = googleResult

    override suspend fun signInWithEmail(email: String, password: String): AppResult<Unit> {
        lastSignInEmail = email
        lastSignInPassword = password
        return emailSignInResult
    }

    override suspend fun createAccountWithEmail(email: String, password: String, username: String): AppResult<Unit> {
        lastCreateAccountEmail = email
        lastCompletedUsername = username
        return createAccountResult
    }

    override fun consumeVerificationDeliveryFailure(): Boolean =
        verificationDeliveryFailurePending.also { verificationDeliveryFailurePending = false }

    override suspend fun sendPasswordReset(email: String): AppResult<Unit> {
        lastPasswordResetEmail = email
        return passwordResetResult
    }

    /** Delay before the verification calls return, to model a slow network. */
    var verificationDelayMs: Long = 0

    override suspend fun sendEmailVerification(): AppResult<Unit> {
        delay(verificationDelayMs)
        return AppResult.Success(Unit)
    }
    override suspend fun refreshEmailVerification(): AppResult<Unit> {
        delay(verificationDelayMs)
        return AppResult.Success(Unit)
    }
    override suspend fun completeProfile(firstName: String, lastName: String, username: String): AppResult<Unit> {
        lastCompletedUsername = username
        return AppResult.Success(Unit)
    }

    override fun currentEmail(): String? = "test@example.com"

    var profileName: ProfileName? = null

    /** Override to delay the read or answer per signed-in account. */
    var profileNameReader: suspend () -> ProfileName? = { profileName }
    override suspend fun currentProfileName(): ProfileName? = profileNameReader()
    override suspend fun requestEmailUpdate(email: String): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun signOut(): AppResult<Unit> {
        signOutCallCount++
        authStateFlow.value = AuthState.SignedOut
        return AppResult.Success(Unit)
    }

    override suspend fun deleteAccount(): AppResult<Unit> {
        deleteAccountCallCount++
        return deleteAccountResult
    }
}
