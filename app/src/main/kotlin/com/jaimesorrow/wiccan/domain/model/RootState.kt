package com.jaimesorrow.wiccan.domain.model

/**
 * Root application state, resolved before the signed-in app is entered.
 * [SignedIn] requires a live Firebase Auth session, a fully initialized
 * server-side profile, current Terms/Privacy acceptance, and a verified,
 * unexpired trial or subscription - never just
 * a non-null current user, since the server-side `users/{uid}` creation
 * trigger runs asynchronously after sign-up and can briefly lag behind
 * the client's auth state, and a returning user may owe acceptance of an
 * updated policy version.
 */
sealed interface RootState {
    data object Loading : RootState
    data object SignedOut : RootState
    data object InitializingProfile : RootState
    data object RequiresPolicyAcceptance : RootState
    data class RequiresSubscription(val uid: String) : RootState
    data class SignedIn(val uid: String) : RootState
}
