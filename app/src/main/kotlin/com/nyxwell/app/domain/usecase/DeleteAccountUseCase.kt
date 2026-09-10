package com.nyxwell.app.domain.usecase

import com.nyxwell.app.core.result.AppResult
import com.nyxwell.app.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Deletes the signed-in user's server-side data and Firebase Auth account,
 * then signs out locally so the root observer routes back to sign-in. Only
 * signs out on a successful delete — a failed delete leaves the session
 * intact so the user can retry.
 */
class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> {
        val result = authRepository.deleteAccount()
        if (result is AppResult.Success) {
            authRepository.signOut()
        }
        return result
    }
}
