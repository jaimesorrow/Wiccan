package com.wiccan.app.core.auth

/**
 * Resolves the currently signed-in user's uid. Kept as an interface so
 * the Today ViewModel can be unit-tested without pulling in Firebase Auth,
 * and so a sign-in flow (out of scope for this pass) can slot in behind
 * the same seam later.
 */
interface CurrentUserProvider {
    val currentUserId: String?
}
