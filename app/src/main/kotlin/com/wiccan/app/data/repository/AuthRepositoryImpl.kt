package com.wiccan.app.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.wiccan.app.analytics.AnalyticsLogger
import com.wiccan.app.core.error.AppError
import com.wiccan.app.core.error.toAppError
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.data.remote.AccountFunctionsDataSource
import com.wiccan.app.data.remote.GoogleCredentialDataSource
import com.wiccan.app.domain.model.AccountStatus
import com.wiccan.app.domain.model.AuthState
import com.wiccan.app.domain.model.PolicyVersions
import com.wiccan.app.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val USERS_COLLECTION = "users"
private const val GOOGLE_WEB_CLIENT_ID_RESOURCE = "default_web_client_id"

class AuthRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
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

    override fun observeAccountStatus(uid: String): Flow<AccountStatus> = callbackFlow {
        val registration = firestore.collection(USERS_COLLECTION).document(uid)
            .addSnapshotListener { snapshot, _ ->
                trySend(
                    when {
                        snapshot?.exists() != true -> AccountStatus.Initializing
                        snapshot.getLong("acceptedTermsVersion")?.toInt() != PolicyVersions.TERMS -> {
                            AccountStatus.RequiresPolicyAcceptance
                        }
                        snapshot.getLong("acceptedPrivacyVersion")?.toInt() != PolicyVersions.PRIVACY -> {
                            AccountStatus.RequiresPolicyAcceptance
                        }
                        else -> AccountStatus.Ready
                    },
                )
            }
        awaitClose { registration.remove() }
    }

    override suspend fun acceptTerms(): AppResult<Unit> = try {
        accountFunctionsDataSource.acceptTerms(PolicyVersions.TERMS, PolicyVersions.PRIVACY)
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }

    override fun isGoogleSignInAvailable(): Boolean = googleWebClientId() != null

    private fun googleWebClientId(): String? {
        // Google Services only generates this resource when OAuth is configured.
        // Resolving it by name keeps email sign-in buildable without that provider.
        val resourceId = appContext.resources.getIdentifier(
            GOOGLE_WEB_CLIENT_ID_RESOURCE,
            "string",
            appContext.packageName,
        )
        if (resourceId == 0) return null
        return appContext.getString(resourceId).takeUnless { it.isBlank() }
    }

    override suspend fun signInWithGoogle(context: Context): AppResult<Unit> {
        return try {
            val webClientId = googleWebClientId()
                ?: return AppResult.Failure(AppError.GoogleSignInUnavailable)
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
