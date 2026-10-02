package com.jaimesorrow.wiccan.domain.model

/**
 * Where a signed-in user's server-side account stands, beyond bare
 * authentication. Drives [RootState] once [AuthState.SignedIn] is true.
 */
sealed interface AccountStatus {
    /** `users/{uid}` hasn't been created by the server-side Auth trigger yet. */
    data object Initializing : AccountStatus

    /** Profile exists, but its accepted policy versions are missing or outdated. */
    data object RequiresPolicyAcceptance : AccountStatus

    data object Ready : AccountStatus
}
