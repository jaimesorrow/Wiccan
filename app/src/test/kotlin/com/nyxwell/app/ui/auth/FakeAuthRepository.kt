package com.nyxwell.app.ui.auth

import android.content.Context
import com.nyxwell.app.core.result.AppResult
import com.nyxwell.app.domain.model.AuthState
import com.nyxwell.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository : AuthRepository {

    private val authStateFlow = MutableStateFlow<AuthState>(AuthState.SignedOut)

    var googleResult: AppResult<Unit> = AppResult.Success(Unit)
    var emailSignInResult: AppResult<Unit> = AppResult.Success(Unit)
    var createAccountResult: AppResult<Unit> = AppResult.Success(Unit)
    var passwordResetResult: AppResult<Unit> = AppResult.Success(Unit)
    var deleteAccountResult: AppResult<Unit> = AppResult.Success(Unit)

    var lastSignInEmail: String? = null
    var lastSignInPassword: String? = null
    var lastCreateAccountEmail: String? = null
    var lastPasswordResetEmail: String? = null
    var signOutCallCount: Int = 0
    var deleteAccountCallCount: Int = 0

    override fun observeAuthState(): Flow<AuthState> = authStateFlow

    override suspend fun signInWithGoogle(context: Context): AppResult<Unit> = googleResult

    override suspend fun signInWithEmail(email: String, password: String): AppResult<Unit> {
        lastSignInEmail = email
        lastSignInPassword = password
        return emailSignInResult
    }

    override suspend fun createAccountWithEmail(email: String, password: String): AppResult<Unit> {
        lastCreateAccountEmail = email
        return createAccountResult
    }

    override suspend fun sendPasswordReset(email: String): AppResult<Unit> {
        lastPasswordResetEmail = email
        return passwordResetResult
    }

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
