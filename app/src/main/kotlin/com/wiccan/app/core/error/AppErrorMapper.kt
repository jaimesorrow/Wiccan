package com.wiccan.app.core.error

/**
 * Single source of truth for turning an [AppError] into user-facing copy.
 * Every screen's snackbar effect goes through this — no per-screen
 * duplicate mappers.
 */
fun AppError.toUserMessage(): String = when (this) {
    is AppError.Network -> "Couldn't connect. Check your connection and try again."
    is AppError.Unauthorized -> "Please sign in again."
    is AppError.NotFound -> "That couldn't be found."
    is AppError.GoogleSignInUnavailable -> "Google sign-in isn't available in this build. Use your email and password."
    is AppError.PurchaseVerificationFailed -> "We couldn't verify that purchase. Try again or contact support."
    is AppError.Unknown -> "Something went wrong."
}
