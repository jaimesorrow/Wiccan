package com.jaimesorrow.wiccan.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.data.local.NotificationPreferences
import com.jaimesorrow.wiccan.data.remote.FcmTokenProvider
import com.jaimesorrow.wiccan.data.remote.NotificationFunctionsDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
    private val auth: FirebaseAuth = mockk()
    private lateinit var repository: NotificationRepositoryImpl

    @Before
    fun setUp() {
        every { auth.currentUser } returns mockk {
            every { uid } returns "account"
        }
        coEvery { preferences.savedChoice("account") } returns null
        repository = NotificationRepositoryImpl(functionsDataSource, tokenProvider, preferences, auth)
    }

    @Test
    fun `registerToken forwards the given token and the device time zone to the callable`() = runTest {
        coEvery { functionsDataSource.registerToken("token-123", any(), "account", null) } returns Unit

        val result = repository.registerToken("token-123")

        assertTrue(result is AppResult.Success)
        coVerify { functionsDataSource.registerToken("token-123", any(), "account", null) }
    }

    @Test
    fun `registerToken classifies a generic failure as Unknown`() = runTest {
        val failure = IllegalStateException("network error")
        coEvery { functionsDataSource.registerToken(any(), any(), any(), any()) } throws failure

        val result = repository.registerToken("token-123")

        assertTrue(result is AppResult.Failure)
        assertEquals(AppError.Unknown("network error"), (result as AppResult.Failure).error)
    }

    @Test
    fun `refreshAndRegisterToken preserves the account opt-out on a replacement token`() = runTest {
        coEvery { tokenProvider.getToken() } returns "fresh-token"
        coEvery { preferences.savedChoice("account") } returns false
        coEvery { functionsDataSource.registerToken("fresh-token", any(), "account", false) } returns Unit

        val result = repository.refreshAndRegisterToken()

        assertTrue(result is AppResult.Success)
        coVerify { functionsDataSource.registerToken("fresh-token", any(), "account", false) }
    }

    @Test
    fun `unregisterToken fetches the current token and forwards it to the callable`() = runTest {
        coEvery { tokenProvider.getToken() } returns "current-token"
        coEvery { functionsDataSource.unregisterToken("current-token", "account") } returns Unit

        val result = repository.unregisterToken()

        assertTrue(result is AppResult.Success)
        coVerify { functionsDataSource.unregisterToken("current-token", "account") }
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
        coEvery { functionsDataSource.registerToken("current-token", any(), "account", false, true) } returns Unit

        val result = repository.setNotificationsEnabled(false)

        assertTrue(result is AppResult.Success)
        coVerify { functionsDataSource.registerToken("current-token", any(), "account", false, true) }
        coVerify { preferences.setNotificationsEnabled("account", false) }
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
