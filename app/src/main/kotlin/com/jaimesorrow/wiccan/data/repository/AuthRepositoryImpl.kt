package com.jaimesorrow.wiccan.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.jaimesorrow.wiccan.analytics.AnalyticsLogger
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.error.toAppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.data.remote.AccountFunctionsDataSource
import com.jaimesorrow.wiccan.data.remote.GoogleCredentialDataSource
import com.jaimesorrow.wiccan.domain.model.AccountStatus
import com.jaimesorrow.wiccan.domain.model.AuthState
import com.jaimesorrow.wiccan.domain.model.PolicyVersions
import com.jaimesorrow.wiccan.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

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
    private val verificationRefresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun observeAuthState(): Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            trySend(if (user != null) AuthState.SignedIn(user.uid) else AuthState.SignedOut)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override fun observeAccountStatus(uid: String): Flow<AccountStatus> = callbackFlow {
        var latestSnapshot: DocumentSnapshot? = null
        fun publish() {
            val snapshot = latestSnapshot
            val user = firebaseAuth.currentUser
            trySend(
                when {
                    snapshot?.exists() != true || user?.uid != uid -> AccountStatus.Initializing
                    !user.isEmailVerified -> AccountStatus.RequiresEmailVerification
                    snapshot.getString("firstName").isNullOrBlank() ||
                        snapshot.getString("lastName").isNullOrBlank() -> AccountStatus.RequiresProfileCompletion
                    snapshot.getLong("acceptedTermsVersion")?.toInt() != PolicyVersions.TERMS ||
                        snapshot.getLong("acceptedPrivacyVersion")?.toInt() != PolicyVersions.PRIVACY ->
                        AccountStatus.RequiresPolicyAcceptance
                    else -> AccountStatus.Ready
                },
            )
        }
        val registration = firestore.collection(USERS_COLLECTION).document(uid)
            .addSnapshotListener { snapshot, _ ->
                latestSnapshot = snapshot
                publish()
            }
        val idTokenListener = FirebaseAuth.IdTokenListener { publish() }
        firebaseAuth.addIdTokenListener(idTokenListener)
        val refreshJob = launch { verificationRefresh.collect { publish() } }
        awaitClose {
            refreshJob.cancel()
            registration.remove()
            firebaseAuth.removeIdTokenListener(idTokenListener)
        }
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
        // Account creation succeeds even if email delivery is temporarily unavailable;
        // the verification screen always offers a retry.
        try { firebaseAuth.currentUser?.sendEmailVerification()?.await() } catch (_: Exception) { }
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

    override suspend fun sendEmailVerification(): AppResult<Unit> = try {
        val user = firebaseAuth.currentUser
        if (user == null) AppResult.Failure(AppError.Unauthorized) else {
            if (!user.isEmailVerified) user.sendEmailVerification().await()
            AppResult.Success(Unit)
        }
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }

    override suspend fun refreshEmailVerification(): AppResult<Unit> = try {
        val user = firebaseAuth.currentUser
        if (user == null) AppResult.Failure(AppError.Unauthorized) else {
            user.reload().await()
            user.getIdToken(true).await()
            verificationRefresh.emit(Unit)
            AppResult.Success(Unit)
        }
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }

    override suspend fun completeProfile(firstName: String, lastName: String): AppResult<Unit> = try {
        accountFunctionsDataSource.completeProfile(firstName, lastName)
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
