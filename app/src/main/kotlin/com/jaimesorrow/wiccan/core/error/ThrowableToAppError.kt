package com.jaimesorrow.wiccan.core.error

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import java.io.IOException
import java.util.concurrent.CancellationException

/** Classifies a caught exception into the shared [AppError] taxonomy. */
fun Throwable.toAppError(): AppError {
    // Coroutine cancellation must not be turned into a snackbar or "unknown" error.
    if (this is CancellationException) throw this
    return when (this) {
        is FirebaseNetworkException -> AppError.Network
        is IOException -> AppError.Network
        is FirebaseFunctionsException -> when (code) {
            FirebaseFunctionsException.Code.UNAUTHENTICATED,
            FirebaseFunctionsException.Code.PERMISSION_DENIED,
            -> AppError.Unauthorized
            FirebaseFunctionsException.Code.NOT_FOUND -> AppError.NotFound
            FirebaseFunctionsException.Code.UNAVAILABLE,
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED,
            -> AppError.Network
            else -> AppError.Unknown(message)
        }
        is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.UNAUTHENTICATED,
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            -> AppError.Unauthorized
            FirebaseFirestoreException.Code.NOT_FOUND -> AppError.NotFound
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            -> AppError.Network
            else -> AppError.Unknown(message)
        }
        is FirebaseAuthInvalidCredentialsException -> when (errorCode) {
            // Email enumeration protection reports a wrong password as INVALID_CREDENTIAL.
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> AppError.InvalidCredentials
            else -> AppError.Unknown(message)
        }
        else -> AppError.Unknown(message)
    }
}
