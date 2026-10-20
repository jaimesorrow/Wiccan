package com.jaimesorrow.wiccan.core.error

/**
 * Single source of truth for turning an [AppError] into user-facing copy.
 * Every screen's snackbar effect goes through this — no per-screen
 * duplicate mappers.
 */
fun AppError.toUserMessage(): String = when (this) {
    is AppError.Network -> "Couldn't connect. Check your connection and try again."
    is AppError.Unauthorized -> "Please sign in again."
    is AppError.NotFound -> "That couldn't be found."
    is AppError.NoNewLesson -> "No new lessons remain. Your previous lessons are saved in Grimoire."
    is AppError.GoogleSignInUnavailable -> "Google sign-in is unavailable. Use your email and password."
    is AppError.InvalidInput -> message
    is AppError.PurchaseVerificationFailed -> "We couldn't verify that purchase. Try again or contact support."
    is AppError.Unknown -> "Something went wrong."
}
