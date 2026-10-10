package com.jaimesorrow.wiccan.data.repository

import app.cash.turbine.test
import com.google.firebase.functions.FirebaseFunctionsException
import com.jaimesorrow.wiccan.core.auth.CurrentUserProvider
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.core.time.LocalCalendarProvider
import com.jaimesorrow.wiccan.data.mapper.toCachedEntity
import com.jaimesorrow.wiccan.data.mapper.toContent
import com.jaimesorrow.wiccan.data.remote.ContentFunctionsDataSource
import com.jaimesorrow.wiccan.domain.model.ContentStatus
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.ContentType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
    private val calendar: LocalCalendarProvider = mockk()
    private var currentUserId: String? = userId
    private val currentUser = object : CurrentUserProvider {
        override val currentUserId: String? get() = this@ContentRepositoryImplTest.currentUserId
    }
    private val today = "2026-10-06"
    private val timezone = "America/Los_Angeles"

    private val payload: Map<String, Any?> = mapOf(
        "id" to "content-1",
        "type" to "spell",
        "tier" to "free",
        "tradition" to "European folk magic",
        "category" to "MoonAndSeasonal",
        "title" to "Candle of Clear Intent",
        "intro" to "A short, grounding ritual.",
        "body" to "Light the candle and speak your intention aloud.",
        "materials" to listOf("white candle", "matches"),
        "safetyNote" to null,
        "tags" to listOf("focus"),
        "status" to "published",
        "deliveredDate" to today,
        "timezone" to timezone,
        "reflection" to "What did you notice?",
        "practice" to "Pause and breathe without lighting a flame.",
        "sourceId" to "source-1",
        "sourceCitation" to "Modern practice guide",
        "sourceUrl" to "https://example.com/source",
        "rightsReviewRequired" to false,
        "rightsNotes" to "Original writing",
    )

    @Before
    fun setUp() {
        dao = FakeCachedDailyContentDao()
        every { calendar.today } returns today
        every { calendar.timezone } returns timezone
        repository = ContentRepositoryImpl(functionsDataSource, dao, currentUser, calendar)
    }

    @Test
    fun `refreshTodayContent maps payload, caches it, and returns success`() = runTest {
        coEvery { functionsDataSource.getTodayContent(timezone) } returns payload

        val result = repository.refreshTodayContent(userId)

        assertTrue(result is AppResult.Success)
        val content = (result as AppResult.Success).data
        assertEquals("content-1", content.id)
        assertEquals(ContentType.SPELL, content.type)
        assertEquals(ContentTier.FREE, content.tier)
        assertEquals(ContentStatus.PUBLISHED, content.status)
        assertEquals(listOf("white candle", "matches"), content.materials)
        assertEquals("What did you notice?", content.reflection)
        assertEquals("Modern practice guide", content.sourceCitation)
        assertEquals("Original writing", content.rightsNotes)
        assertEquals(today, dao.get()?.deliveredDate)
        assertEquals(timezone, dao.get()?.timezone)
        assertEquals(userId, dao.get()?.userId)
        coVerify(exactly = 1) { functionsDataSource.getTodayContent(timezone) }

        repository.observeTodayContent().test {
            val cached = awaitItem()
            assertEquals("content-1", cached?.id)
        }
    }

    @Test
    fun `refreshTodayContent classifies a generic failure as Unknown, without touching the cache`() = runTest {
        val failure = IllegalStateException("Cloud Function unavailable")
        coEvery { functionsDataSource.getTodayContent(timezone) } throws failure

        val result = repository.refreshTodayContent(userId)

        assertTrue(result is AppResult.Failure)
        assertEquals(AppError.Unknown("Cloud Function unavailable"), (result as AppResult.Failure).error)

        repository.observeTodayContent().test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `resource exhausted response signals that no new lesson is available`() = runTest {
        val failure = mockk<FirebaseFunctionsException>()
        every { failure.code } returns FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED
        every { failure.details } returns mapOf("reason" to "NO_NEW_CONTENT")
        coEvery { functionsDataSource.getTodayContent(timezone) } throws failure

        val result = repository.refreshTodayContent(userId)

        assertEquals(AppError.NoNewLesson, (result as AppResult.Failure).error)
        repository.observeTodayContent().test { assertNull(awaitItem()) }
    }

    @Test
    fun `the cache keeps the authoritative assignment date rather than stamping the client date`() = runTest {
        coEvery { functionsDataSource.getTodayContent(timezone) } returns (payload + ("deliveredDate" to "2026-10-05"))
        repository.refreshTodayContent(userId)
        assertEquals("2026-10-05", dao.get()?.deliveredDate)
        repository.observeTodayContent().test { assertNull(awaitItem()) }
    }

    @Test
    fun `previous day cache is hidden and invalidated even when refresh is offline`() = runTest {
        dao.upsert(payload.toContent().toCachedEntity("2026-10-05", userId, timezone))
        repository.observeTodayContent().test { assertNull(awaitItem()) }
        coEvery { functionsDataSource.getTodayContent(timezone) } throws IllegalStateException("Offline")
        repository.refreshTodayContent(userId)
        assertNull(dao.get())
    }

    @Test
    fun `other users cached lessons are never exposed`() = runTest {
        dao.upsert(payload.toContent().toCachedEntity(today, "other-user", timezone))
        repository.observeTodayContent().test { assertNull(awaitItem()) }
    }

    @Test
    fun `exhaustion clears only the daily cache while Grimoire history remains server owned`() = runTest {
        dao.upsert(payload.toContent().toCachedEntity(today, userId, timezone))
        val failure = mockk<FirebaseFunctionsException>()
        every { failure.code } returns FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED
        every { failure.details } returns mapOf("reason" to "NO_NEW_CONTENT")
        coEvery { functionsDataSource.getTodayContent(timezone) } throws failure
        assertEquals(AppError.NoNewLesson, (repository.refreshTodayContent(userId) as AppResult.Failure).error)
        assertNull(dao.get())
    }

    @Test
    fun `temporarily unavailable assignment keeps today's cached lesson`() = runTest {
        val cached = payload.toContent().toCachedEntity(today, userId, timezone)
        dao.upsert(cached)
        val failure = mockk<FirebaseFunctionsException>()
        every { failure.code } returns FirebaseFunctionsException.Code.FAILED_PRECONDITION
        every { failure.details } returns mapOf("reason" to "ASSIGNED_CONTENT_UNAVAILABLE")
        coEvery { functionsDataSource.getTodayContent(timezone) } throws failure

        val result = repository.refreshTodayContent(userId)

        assertEquals(
            AppError.Unknown("Today's assigned content is temporarily unavailable."),
            (result as AppResult.Failure).error,
        )
        assertEquals(cached, dao.get())
    }

    @Test
    fun `an account switch during assignment prevents another users lesson from being cached`() = runTest {
        coEvery { functionsDataSource.getTodayContent(timezone) } answers {
            currentUserId = "other-user"
            payload
        }
        assertEquals(AppError.Unauthorized, (repository.refreshTodayContent(userId) as AppResult.Failure).error)
        assertNull(dao.get())
    }

    @Test
    fun `a restored unowned cache is hidden for both signed out and signed in users`() = runTest {
        val legacy = payload.toContent().toCachedEntity(today, userId, timezone).copy(userId = null, timezone = null)
        dao.upsert(legacy)
        currentUserId = null
        repository.observeTodayContent().test { assertNull(awaitItem()) }
        currentUserId = userId
        repository.observeTodayContent().test { assertNull(awaitItem()) }
        assertEquals(legacy, dao.get())
    }

    @Test
    fun `a payload lacking server assignment metadata is not cached`() = runTest {
        coEvery { functionsDataSource.getTodayContent(timezone) } returns (payload - "deliveredDate")
        assertTrue(repository.refreshTodayContent(userId) is AppResult.Failure)
        assertNull(dao.get())
    }
}
