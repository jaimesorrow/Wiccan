package com.nyxwell.app.core.error

/**
 * User-facing error surface for snackbar effects. Kept as a small sealed
 * hierarchy so screens can react to specific cases (offer sign-in, offer
 * retry) without coupling to underlying exception types.
 */
sealed class AppError {
    /** No signed-in user — the callable Cloud Function needs an authenticated uid. */
    data object NoUser : AppError()

    /** Any refresh failure surfaced from the repository. */
    data class Refresh(val cause: Throwable) : AppError()

    /** Any sign-in, account-creation, or password-reset failure. */
    data class Auth(val cause: Throwable) : AppError()

    /** A generic fetch failure (e.g. loading one archived item's detail). */
    data class LoadFailed(val cause: Throwable) : AppError()
}
