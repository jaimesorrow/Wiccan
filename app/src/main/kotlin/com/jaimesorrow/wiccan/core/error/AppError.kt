package com.jaimesorrow.wiccan.core.error

/**
 * Shared, kind-based error surface for snackbar effects. Repositories
 * classify failures into these cases; screens react generically (offer
 * retry, route to sign-in) without coupling to underlying exception types.
 */
sealed class AppError {
    object Network : AppError()
    object Unauthorized : AppError()
    object NotFound : AppError()
    object NoNewLesson : AppError()
    object SubscriptionRequired : AppError()
    object GoogleSignInUnavailable : AppError()

    /** The user dismissed a system flow (e.g. the account picker); screens show nothing. */
    object Cancelled : AppError()
    data class PurchaseVerificationFailed(val reason: String? = null) : AppError()

    /** Email/password sign-in rejected the password or credential. */
    object InvalidCredentials : AppError()

    /** The server sent content (for example a new content type) that this build can't display. */
    object UnsupportedContent : AppError()

    /** Restore Purchases could not be completed for a reason other than the network or auth. */
    object RestoreFailed : AppError()
    data class Unknown(val message: String? = null) : AppError()
}
