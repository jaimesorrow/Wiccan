package com.nyxwell.app.data.repository

import com.nyxwell.app.core.result.AppResult
import com.nyxwell.app.data.remote.FcmTokenProvider
import com.nyxwell.app.data.remote.NotificationFunctionsDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationRepositoryImplTest {

    private val functionsDataSource: NotificationFunctionsDataSource = mockk()
    private val tokenProvider: FcmTokenProvider = mockk()
    private lateinit var repository: NotificationRepositoryImpl

    @Before
    fun setUp() {
        repository = NotificationRepositoryImpl(functionsDataSource, tokenProvider)
    }

    @Test
    fun `registerToken forwards the given token to the callable`() = runTest {
        coEvery { functionsDataSource.registerToken("token-123") } returns Unit

        val result = repository.registerToken("token-123")

        assertTrue(result is AppResult.Success)
        coVerify { functionsDataSource.registerToken("token-123") }
    }

    @Test
    fun `registerToken surfaces failures`() = runTest {
        val failure = IllegalStateException("network error")
        coEvery { functionsDataSource.registerToken(any()) } throws failure

        val result = repository.registerToken("token-123")

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).throwable === failure)
    }

    @Test
    fun `refreshAndRegisterToken fetches the current token then registers it`() = runTest {
        coEvery { tokenProvider.getToken() } returns "fresh-token"
        coEvery { functionsDataSource.registerToken("fresh-token") } returns Unit

        val result = repository.refreshAndRegisterToken()

        assertTrue(result is AppResult.Success)
        coVerify { functionsDataSource.registerToken("fresh-token") }
    }
}
