package com.wiccan.app.data.repository

import com.wiccan.app.core.error.toAppError
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.data.remote.FcmTokenProvider
import com.wiccan.app.data.remote.NotificationFunctionsDataSource
import com.wiccan.app.domain.repository.NotificationRepository
import java.time.ZoneId
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val functionsDataSource: NotificationFunctionsDataSource,
    private val tokenProvider: FcmTokenProvider,
) : NotificationRepository {

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
        functionsDataSource.unregisterToken()
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }
}
