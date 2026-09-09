package com.nyxwell.app.core.auth

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class FirebaseCurrentUserProvider @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : CurrentUserProvider {
    override val currentUserId: String? get() = firebaseAuth.currentUser?.uid
}
