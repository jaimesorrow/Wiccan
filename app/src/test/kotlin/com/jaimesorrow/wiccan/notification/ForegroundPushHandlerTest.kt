package com.jaimesorrow.wiccan.notification

import com.jaimesorrow.wiccan.core.auth.CurrentUserProvider
import com.jaimesorrow.wiccan.domain.repository.NotificationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundPushHandlerTest {

    private val content = ForegroundNotificationContent("Today's spell", "New Moon Intention Spell")
    private val reminderOn = MutableStateFlow(true)
    private var uid: String? = "uid-a"
    private val repository: NotificationRepository = mockk {
        every { notificationsEnabled } returns reminderOn
    }
    private val users = object : CurrentUserProvider {
        override val currentUserId: String? get() = uid
    }
    private val poster: ForegroundNotificationPoster = mockk {
        every { post(any()) } returns true
    }
    private val handler = ForegroundPushHandler(repository, users, poster)

    @Test
    fun `posts while signed in with the daily reminder on`() = runTest {
        assertTrue(handler.handle(content))
        verify(exactly = 1) { poster.post(content) }
    }

    @Test
    fun `an in-flight push after sign-out is dropped`() = runTest {
        uid = null
        reminderOn.value = false // the repository reports false when signed out

        assertFalse(handler.handle(content))
        verify(exactly = 0) { poster.post(any()) }
    }

    @Test
    fun `an in-flight push after the reminder is turned off is dropped`() = runTest {
        reminderOn.value = false

        assertFalse(handler.handle(content))
        verify(exactly = 0) { poster.post(any()) }
    }

    @Test
    fun `signed out is dropped even if the cached choice still says on`() = runTest {
        uid = null

        assertFalse(handler.handle(content))
        verify(exactly = 0) { poster.post(any()) }
    }

    @Test
    fun `an unreadable reminder choice is treated as off`() = runTest {
        every { repository.notificationsEnabled } returns emptyFlow()

        assertFalse(handler.handle(content))
        verify(exactly = 0) { poster.post(any()) }
    }
}
