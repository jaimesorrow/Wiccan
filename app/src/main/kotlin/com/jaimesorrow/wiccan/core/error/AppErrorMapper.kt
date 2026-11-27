package com.jaimesorrow.wiccan.core.error

import android.content.res.Resources
import androidx.annotation.StringRes
import com.wiccan.app.R

/**
 * Single source of truth for turning an [AppError] into user-facing copy.
 * Every screen's snackbar effect goes through this — no per-screen
 * duplicate mappers. Wording lives in `strings.xml`. Callers in Compose
 * should pass [androidx.compose.ui.platform.LocalResources].
 */
fun AppError.toUserMessage(resources: Resources): String = resources.getString(userMessageRes())

@StringRes
fun AppError.userMessageRes(): Int = when (this) {
    is AppError.Network -> R.string.error_network
    is AppError.Unauthorized -> R.string.error_unauthorized
    is AppError.NotFound -> R.string.error_not_found
    is AppError.NoNewLesson -> R.string.error_no_new_lesson
    is AppError.SubscriptionRequired -> R.string.error_subscription_required
    is AppError.GoogleSignInUnavailable -> R.string.error_google_sign_in
    is AppError.VerificationEmailDeliveryFailed -> R.string.error_verification_email_delivery
    is AppError.Cancelled -> R.string.error_cancelled
    is AppError.PurchaseVerificationFailed -> R.string.error_purchase_verification
    is AppError.InvalidCredentials -> R.string.error_invalid_credentials
    is AppError.UsernameTaken -> R.string.error_username_taken
    is AppError.RestoreFailed -> R.string.error_restore_purchases
    is AppError.UnsupportedContent -> R.string.error_unsupported_content
    is AppError.Unknown -> R.string.error_unknown
}
