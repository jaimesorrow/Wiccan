package com.jaimesorrow.wiccan.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
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
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val USERS_COLLECTION = "users"
private const val GOOGLE_WEB_CLIENT_ID_RESOURCE = "default_web_client_id"

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

    override suspend fun signInWithGoogle(context: Context): AppResult<Unit> {
        val resourceId = context.resources.getIdentifier(
            GOOGLE_WEB_CLIENT_ID_RESOURCE,
            "string",
            context.packageName,
        )
        if (resourceId == 0) {
            return AppResult.Failure(
                AppError.Unknown("Google Sign-In is not configured for this build."),
            )
        }

        return try {
            val webClientId = context.getString(resourceId)
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
