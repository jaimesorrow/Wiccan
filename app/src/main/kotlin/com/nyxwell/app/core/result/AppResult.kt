package com.nyxwell.app.core.result

/**
 * Generic outcome wrapper for repository operations that can fail
 * (network, Cloud Function errors, etc). Kept out of UiState per the
 * MVI convention here: failures surface via UiEffect, not state fields.
 */
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val throwable: Throwable, val message: String? = throwable.message) : AppResult<Nothing>()
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onError(action: (Throwable) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(throwable)
    return this
}
