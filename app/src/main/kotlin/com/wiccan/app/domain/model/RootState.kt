package com.wiccan.app.domain.model

/**
 * Root application state, resolved before the signed-in app is entered.
 * [SignedIn] requires both a live Firebase Auth session *and* a fully
 * initialized server-side profile - never just a non-null current user,
 * since the server-side `users/{uid}` creation trigger runs asynchronously
 * after sign-up and can briefly lag behind the client's auth state.
 *
 * [RequiresPolicyAcceptance] is reserved for the versioned Terms/Privacy
 * acceptance flow: no data source populates the acceptance fields it needs
 * yet, so [RootViewModel][com.wiccan.app.ui.auth.RootViewModel] never
 * currently emits it. Wiring that check in is tracked as its own follow-up
 * rather than faked here.
 */
sealed interface RootState {
    data object Loading : RootState
    data object SignedOut : RootState
    data object InitializingProfile : RootState
    data object RequiresPolicyAcceptance : RootState
    data object SignedIn : RootState
}
