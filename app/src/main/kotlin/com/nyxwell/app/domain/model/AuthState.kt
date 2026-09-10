package com.nyxwell.app.domain.model

sealed class AuthState {
    data object SignedOut : AuthState()
    data class SignedIn(val uid: String) : AuthState()
}
