package com.wiccan.app.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.wiccan.app.R
import com.wiccan.app.analytics.AnalyticsLogger
import com.wiccan.app.core.error.toAppError
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.data.remote.AccountFunctionsDataSource
import com.wiccan.app.data.remote.GoogleCredentialDataSource
import com.wiccan.app.domain.model.AuthState
import com.wiccan.app.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val USERS_COLLECTION = "users"

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val googleCredentialDataSource: GoogleCredentialDataSource,
    private val accountFunctionsDataSource: AccountFunctionsDataSource,
    private val analyticsLogger: AnalyticsLogger,
) : AuthRepository {

    override fun observeAuthState(): Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            trySend(if (user != null) AuthState.SignedIn(user.uid) else AuthState.SignedOut)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override fun observeProfileReady(uid: String): Flow<Boolean> = callbackFlow {
        val registration = firestore.collection(USERS_COLLECTION).document(uid)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.exists() == true)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun signInWithGoogle(context: Context): AppResult<Unit> = try {
        val webClientId = context.getString(R.string.default_web_client_id)
        val idToken = googleCredentialDataSource.requestGoogleIdToken(context, webClientId)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()
        if (result.additionalUserInfo?.isNewUser == true) {
            analyticsLogger.logSignUpCompleted()
        }
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }

    override suspend fun signInWithEmail(email: String, password: String): AppResult<Unit> = try {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }

    override suspend fun createAccountWithEmail(email: String, password: String): AppResult<Unit> = try {
        firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        analyticsLogger.logSignUpCompleted()
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }

    override suspend fun sendPasswordReset(email: String): AppResult<Unit> = try {
        firebaseAuth.sendPasswordResetEmail(email).await()
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }

    override suspend fun signOut(): AppResult<Unit> = try {
        firebaseAuth.signOut()
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }

    override suspend fun deleteAccount(): AppResult<Unit> = try {
        accountFunctionsDataSource.deleteUserData()
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }
}
