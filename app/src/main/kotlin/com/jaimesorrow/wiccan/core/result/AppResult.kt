package com.jaimesorrow.wiccan.core.result

import com.jaimesorrow.wiccan.core.error.AppError

/**
 * Generic outcome wrapper for repository operations that can fail
 * (network, Cloud Function errors, etc). Kept out of UiState per the
 * MVI convention here: failures surface via UiEffect, not state fields.
 */
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Failure(val error: AppError) : AppResult<Nothing>()
}
