package com.jaimesorrow.wiccan.data.repository

import com.jaimesorrow.wiccan.core.error.toAppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.data.local.NotificationPreferences
import com.jaimesorrow.wiccan.data.remote.FcmTokenProvider
import com.jaimesorrow.wiccan.data.remote.NotificationFunctionsDataSource
import com.jaimesorrow.wiccan.domain.repository.NotificationRepository
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class NotificationRepositoryImpl @Inject constructor(
    private val functionsDataSource: NotificationFunctionsDataSource,
    private val tokenProvider: FcmTokenProvider,
    private val preferences: NotificationPreferences,
) : NotificationRepository {

    override val notificationsEnabled: Flow<Boolean> = preferences.notificationsEnabled

    override suspend fun registerToken(token: String): AppResult<Unit> = try {
        functionsDataSource.registerToken(token, ZoneId.systemDefault().id)
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }

    override suspend fun refreshAndRegisterToken(): AppResult<Unit> = try {
        val token = tokenProvider.getToken()
        functionsDataSource.registerToken(token, ZoneId.systemDefault().id)
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }

    override suspend fun unregisterToken(): AppResult<Unit> = try {
        val token = tokenProvider.getToken()
        functionsDataSource.unregisterToken(token)
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean): AppResult<Unit> = try {
        val token = tokenProvider.getToken()
        functionsDataSource.setNotificationsEnabled(token, enabled)
        preferences.setNotificationsEnabled(enabled)
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }
}
