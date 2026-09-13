package com.wiccan.app.data.repository

import com.wiccan.app.core.error.AppError
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.data.local.NotificationPreferences
import com.wiccan.app.data.remote.FcmTokenProvider
import com.wiccan.app.data.remote.NotificationFunctionsDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationRepositoryImplTest {

    private val functionsDataSource: NotificationFunctionsDataSource = mockk()
    private val tokenProvider: FcmTokenProvider = mockk()
    private val preferences: NotificationPreferences = mockk(relaxed = true)
    private lateinit var repository: NotificationRepositoryImpl

    @Before
    fun setUp() {
        repository = NotificationRepositoryImpl(functionsDataSource, tokenProvider, preferences)
    }

    @Test
    fun `registerToken forwards the given token and the device time zone to the callable`() = runTest {
        coEvery { functionsDataSource.registerToken("token-123", any()) } returns Unit

        val result = repository.registerToken("token-123")

        assertTrue(result is AppResult.Success)
        coVerify { functionsDataSource.registerToken("token-123", any()) }
    }

    @Test
    fun `registerToken classifies a generic failure as Unknown`() = runTest {
        val failure = IllegalStateException("network error")
        coEvery { functionsDataSource.registerToken(any(), any()) } throws failure

        val result = repository.registerToken("token-123")

        assertTrue(result is AppResult.Failure)
        assertEquals(AppError.Unknown("network error"), (result as AppResult.Failure).error)
    }

    @Test
    fun `refreshAndRegisterToken fetches the current token then registers it`() = runTest {
        coEvery { tokenProvider.getToken() } returns "fresh-token"
        coEvery { functionsDataSource.registerToken("fresh-token", any()) } returns Unit

        val result = repository.refreshAndRegisterToken()

        assertTrue(result is AppResult.Success)
        coVerify { functionsDataSource.registerToken("fresh-token", any()) }
    }

    @Test
    fun `unregisterToken fetches the current token and forwards it to the callable`() = runTest {
        coEvery { tokenProvider.getToken() } returns "current-token"
        coEvery { functionsDataSource.unregisterToken("current-token") } returns Unit

        val result = repository.unregisterToken()

        assertTrue(result is AppResult.Success)
        coVerify { functionsDataSource.unregisterToken("current-token") }
    }

    @Test
    fun `unregisterToken classifies a generic failure as Unknown`() = runTest {
        val failure = IllegalStateException("network error")
        coEvery { tokenProvider.getToken() } throws failure

        val result = repository.unregisterToken()

        assertTrue(result is AppResult.Failure)
        assertEquals(AppError.Unknown("network error"), (result as AppResult.Failure).error)
    }

    @Test
    fun `setNotificationsEnabled forwards the current token and value, then updates the local cache`() = runTest {
        coEvery { tokenProvider.getToken() } returns "current-token"
        coEvery { functionsDataSource.setNotificationsEnabled("current-token", false) } returns Unit

        val result = repository.setNotificationsEnabled(false)

        assertTrue(result is AppResult.Success)
        coVerify { functionsDataSource.setNotificationsEnabled("current-token", false) }
        coVerify { preferences.setNotificationsEnabled(false) }
    }

    @Test
    fun `setNotificationsEnabled classifies a generic failure as Unknown`() = runTest {
        val failure = IllegalStateException("network error")
        coEvery { tokenProvider.getToken() } throws failure

        val result = repository.setNotificationsEnabled(false)

        assertTrue(result is AppResult.Failure)
        assertEquals(AppError.Unknown("network error"), (result as AppResult.Failure).error)
    }
}
