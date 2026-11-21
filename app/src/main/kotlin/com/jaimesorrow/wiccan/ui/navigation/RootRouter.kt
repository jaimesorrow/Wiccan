package com.jaimesorrow.wiccan.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.jaimesorrow.wiccan.domain.model.RootState

/**
 * MainActivity's top-level routing. The signed-in content (the app shell and
 * its NavController/back stack) exists only while [rootState] is
 * [RootState.SignedIn], keyed by uid: signing out disposes it, so Back on the
 * signed-out screen can never return into it, and the next sign-in starts a
 * fresh back stack.
 */
@Composable
internal fun RootRouter(
    rootState: RootState,
    signedIn: @Composable (uid: String) -> Unit,
    signedOut: @Composable () -> Unit,
    other: @Composable (RootState) -> Unit,
) {
    when (rootState) {
        is RootState.SignedIn -> key(rootState.uid) { signedIn(rootState.uid) }
        RootState.SignedOut -> signedOut()
        else -> other(rootState)
    }
}
