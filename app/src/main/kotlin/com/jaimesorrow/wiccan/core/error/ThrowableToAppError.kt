package com.jaimesorrow.wiccan.core.error

import com.google.firebase.FirebaseNetworkException
import java.io.IOException

/** Classifies a caught exception into the shared [AppError] taxonomy. */
fun Throwable.toAppError(): AppError = when (this) {
    is FirebaseNetworkException -> AppError.Network
    is IOException -> AppError.Network
    else -> AppError.Unknown(message)
}
