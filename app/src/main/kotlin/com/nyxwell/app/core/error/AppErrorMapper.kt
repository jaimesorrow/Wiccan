package com.nyxwell.app.core.error

/**
 * Single source of truth for turning an [AppError] into user-facing copy.
 * Every screen's snackbar effect goes through this — no per-screen
 * duplicate mappers.
 */
fun AppError.toUserMessage(): String = when (this) {
    AppError.NoUser -> "Sign in to see today's content."
    is AppError.Refresh -> "Couldn't fetch today's content. Please try again."
    is AppError.Auth -> "That didn't work — check your details and try again."
    is AppError.LoadFailed -> "Couldn't load that. Please try again."
    is AppError.DeleteAccountFailed -> "Couldn't delete your account. Please try again."
}
