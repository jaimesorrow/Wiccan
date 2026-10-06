package com.jaimesorrow.wiccan.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.jaimesorrow.wiccan.core.auth.CurrentUserProvider
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.domain.repository.ContentRepository
import com.jaimesorrow.wiccan.widget.WidgetUpdater
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyContentWorkerTest {
    private val repository: ContentRepository = mockk()
    private val updater: WidgetUpdater = mockk(relaxed = true)
    private val currentUser = object : CurrentUserProvider {
        override val currentUserId = "user-1"
    }
    private val worker =
        DailyContentWorker(mockk<Context>(relaxed = true), mockk<WorkerParameters>(relaxed = true), repository, currentUser, updater)

    @Test
    fun `exhaustion completes scheduled work and refreshes widget from cleared cache`() = runTest {
        coEvery { repository.refreshTodayContent("user-1") } returns AppResult.Failure(AppError.NoNewLesson)
        assertEquals(ListenableWorker.Result.success(), worker.doWork())
        coVerify(exactly = 1) { updater.updateAll(any()) }
    }

    @Test
    fun `temporary failure retries rather than marking the day complete`() = runTest {
        coEvery { repository.refreshTodayContent("user-1") } returns AppResult.Failure(AppError.Network)
        assertEquals(ListenableWorker.Result.retry(), worker.doWork())
        coVerify(exactly = 0) { updater.updateAll(any()) }
    }
}
