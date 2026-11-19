package com.jaimesorrow.wiccan.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.error.toAppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.data.local.NotificationPreferences
import com.jaimesorrow.wiccan.data.remote.FcmTokenProvider
import com.jaimesorrow.wiccan.data.remote.NotificationFunctionsDataSource
import com.jaimesorrow.wiccan.domain.repository.NotificationRepository
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NotificationRepositoryImpl @Inject constructor(
    private val functionsDataSource: NotificationFunctionsDataSource,
    private val tokenProvider: FcmTokenProvider,
    private val preferences: NotificationPreferences,
    private val auth: FirebaseAuth,
) : NotificationRepository {

    private val writes = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val notificationsEnabled: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged().flatMapLatest { uid ->
        if (uid == null) flowOf(false) else preferences.observe(uid)
    }

    override suspend fun registerToken(token: String): AppResult<Unit> = writeForAccount { uid ->
        register(token, uid)
    }

    override suspend fun refreshAndRegisterToken(): AppResult<Unit> = writeForAccount { uid ->
        register(tokenProvider.getToken(), uid)
    }

    private suspend fun register(token: String, uid: String) {
        val enabled = preferences.savedChoice(uid)
        functionsDataSource.registerToken(token, ZoneId.systemDefault().id, uid, enabled)
    }

    override suspend fun unregisterToken(): AppResult<Unit> = writeForAccount { uid ->
        functionsDataSource.unregisterToken(tokenProvider.getToken(), uid)
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean): AppResult<Unit> = writeForAccount { uid ->
        val token = tokenProvider.getToken()
        functionsDataSource.registerToken(token, ZoneId.systemDefault().id, uid, enabled, updateChoice = true)
        preferences.setNotificationsEnabled(uid, enabled)
    }

    private suspend fun writeForAccount(action: suspend (String) -> Unit): AppResult<Unit> {
        val uid = auth.currentUser?.uid ?: return AppResult.Failure(AppError.Unauthorized)
        return writes.withLock {
            if (auth.currentUser?.uid != uid) return@withLock AppResult.Failure(AppError.Unauthorized)
            try {
                action(uid)
                AppResult.Success(Unit)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                AppResult.Failure(t.toAppError())
            }
        }
    }
}
