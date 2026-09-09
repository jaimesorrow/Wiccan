package com.nyxwell.app.domain.result

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>

    data class Error(val throwable: Throwable) : AppResult<Nothing>
}
