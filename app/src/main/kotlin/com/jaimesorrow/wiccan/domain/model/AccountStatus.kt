package com.jaimesorrow.wiccan.domain.model

import com.jaimesorrow.wiccan.core.error.AppError

/**
 * Where a signed-in user's server-side account stands, beyond bare
 * authentication. Drives [RootState] once [AuthState.SignedIn] is true.
 */
sealed interface AccountStatus {
    /** `users/{uid}` hasn't been created by the server-side Auth trigger yet. */
    data object Initializing : AccountStatus

    /** Profile exists, but its accepted policy versions are missing or outdated. */
    data object RequiresPolicyAcceptance : AccountStatus

    data object RequiresEmailVerification : AccountStatus
    data object RequiresProfileCompletion : AccountStatus
    data object Ready : AccountStatus

    /** The profile listener failed; [error] is retryable from the root screen. */
    data class Unavailable(val error: AppError) : AccountStatus
}
