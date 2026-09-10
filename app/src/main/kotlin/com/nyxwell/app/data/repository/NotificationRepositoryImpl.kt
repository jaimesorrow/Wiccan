package com.nyxwell.app.data.repository

import com.nyxwell.app.core.error.toAppError
import com.nyxwell.app.core.result.AppResult
import com.nyxwell.app.data.remote.FcmTokenProvider
import com.nyxwell.app.data.remote.NotificationFunctionsDataSource
import com.nyxwell.app.domain.repository.NotificationRepository
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val functionsDataSource: NotificationFunctionsDataSource,
    private val tokenProvider: FcmTokenProvider,
) : NotificationRepository {

    override suspend fun registerToken(token: String): AppResult<Unit> = try {
        functionsDataSource.registerToken(token)
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }

    override suspend fun refreshAndRegisterToken(): AppResult<Unit> = try {
        val token = tokenProvider.getToken()
        functionsDataSource.registerToken(token)
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        AppResult.Failure(t.toAppError())
    }
}
