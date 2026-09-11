package com.wiccan.app.core.error

/**
 * Shared, kind-based error surface for snackbar effects. Repositories
 * classify failures into these cases; screens react generically (offer
 * retry, route to sign-in) without coupling to underlying exception types.
 */
sealed class AppError {
    object Network : AppError()
    object Unauthorized : AppError()
    object NotFound : AppError()
    data class PurchaseVerificationFailed(val reason: String? = null) : AppError()
    data class Unknown(val message: String? = null) : AppError()
}
