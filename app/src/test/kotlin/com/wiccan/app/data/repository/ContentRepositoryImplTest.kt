package com.wiccan.app.data.repository

import app.cash.turbine.test
import com.wiccan.app.core.error.AppError
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.data.remote.ContentFunctionsDataSource
import com.wiccan.app.domain.model.ContentStatus
import com.wiccan.app.domain.model.ContentTier
import com.wiccan.app.domain.model.ContentType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContentRepositoryImplTest {

    private val functionsDataSource: ContentFunctionsDataSource = mockk()
    private lateinit var dao: FakeCachedDailyContentDao
    private lateinit var repository: ContentRepositoryImpl

    private val userId = "user-123"

    private val payload: Map<String, Any?> = mapOf(
        "id" to "content-1",
        "type" to "spell",
        "tier" to "free",
        "tradition" to "European folk magic",
        "title" to "Candle of Clear Intent",
        "intro" to "A short, grounding ritual.",
        "body" to "Light the candle and speak your intention aloud.",
        "materials" to listOf("white candle", "matches"),
        "safetyNote" to null,
        "tags" to listOf("focus", "beginner"),
        "status" to "published"
    )

    @Before
    fun setUp() {
        dao = FakeCachedDailyContentDao()
        repository = ContentRepositoryImpl(functionsDataSource, dao)
    }

    @Test
    fun `refreshTodayContent maps payload, caches it, and returns success`() = runTest {
        coEvery { functionsDataSource.getTodayContent() } returns payload

        val result = repository.refreshTodayContent(userId)

        assertTrue(result is AppResult.Success)
        val content = (result as AppResult.Success).data
        assertEquals("content-1", content.id)
        assertEquals(ContentType.SPELL, content.type)
        assertEquals(ContentTier.FREE, content.tier)
        assertEquals(ContentStatus.PUBLISHED, content.status)
        assertEquals(listOf("white candle", "matches"), content.materials)

        repository.observeTodayContent().test {
            val cached = awaitItem()
            assertEquals("content-1", cached?.id)
        }
    }

    @Test
    fun `refreshTodayContent classifies a generic failure as Unknown, without touching the cache`() = runTest {
        val failure = IllegalStateException("Cloud Function unavailable")
        coEvery { functionsDataSource.getTodayContent() } throws failure

        val result = repository.refreshTodayContent(userId)

        assertTrue(result is AppResult.Failure)
        assertEquals(AppError.Unknown("Cloud Function unavailable"), (result as AppResult.Failure).error)

        repository.observeTodayContent().test {
            assertNull(awaitItem())
        }
    }
}
