package com.jaimesorrow.wiccan.core.error

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import java.io.IOException

/** Classifies a caught exception into the shared [AppError] taxonomy. */
fun Throwable.toAppError(): AppError = when (this) {
    is FirebaseNetworkException -> AppError.Network
    is IOException -> AppError.Network
    is FirebaseAuthInvalidCredentialsException -> when (errorCode) {
        // Email enumeration protection reports a wrong password as INVALID_CREDENTIAL.
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> AppError.InvalidCredentials
        else -> AppError.Unknown(message)
    }
    else -> AppError.Unknown(message)
}
