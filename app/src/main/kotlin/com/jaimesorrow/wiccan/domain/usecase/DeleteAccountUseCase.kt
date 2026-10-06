package com.jaimesorrow.wiccan.domain.usecase

import com.jaimesorrow.wiccan.analytics.AnalyticsLogger
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Deletes the signed-in user's server-side data and Firebase Auth account,
 * then signs out locally so the root observer routes back to sign-in. Only
 * signs out on a successful delete — a failed delete leaves the session
 * intact so the user can retry.
 */
class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val analyticsLogger: AnalyticsLogger,
) {
    suspend operator fun invoke(): AppResult<Unit> {
        val result = authRepository.deleteAccount()
        if (result is AppResult.Success) {
            analyticsLogger.logAccountDeleted()
            authRepository.signOut()
        }
        return result
    }
}
