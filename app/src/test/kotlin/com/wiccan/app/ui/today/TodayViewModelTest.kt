package com.wiccan.app.ui.today

import app.cash.turbine.test
import com.wiccan.app.analytics.AnalyticsLogger
import com.wiccan.app.core.auth.CurrentUserProvider
import com.wiccan.app.core.error.AppError
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.domain.model.Content
import com.wiccan.app.domain.model.ContentCategory
import com.wiccan.app.domain.model.ContentStatus
import com.wiccan.app.domain.model.ContentTier
import com.wiccan.app.domain.model.ContentType
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeContentRepository
    private val analyticsLogger: AnalyticsLogger = mockk(relaxed = true)

    private val sampleContent = Content(
        id = "content-1",
        type = ContentType.SPELL,
        tier = ContentTier.FREE,
        tradition = "European folk magic",
        category = ContentCategory.MOON_AND_SEASONAL,
        title = "Candle of Clear Intent",
        intro = "A short, grounding ritual.",
        body = "Light the candle and speak your intention aloud.",
        materials = listOf("white candle", "matches"),
        safetyNote = null,
        tags = listOf("focus"),
        status = ContentStatus.PUBLISHED
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeContentRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful init refresh flips isLoading false and surfaces content`() = runTest(dispatcher) {
        repository.nextResult = AppResult.Success(sampleContent)
        val viewModel = TodayViewModel(repository, currentUser("user-1"), analyticsLogger)

        viewModel.state.test {
            val initial = awaitItem()
            assertTrue(initial.isLoading)
            assertNull(initial.content)

            // Drain state updates until refresh completes and content lands.
            var latest = initial
            while (latest.isLoading || latest.content == null) {
                latest = awaitItem()
            }
            assertEquals(sampleContent, latest.content)
            assertFalse(latest.isLoading)
            cancelAndConsumeRemainingEvents()
        }

        assertEquals(listOf("user-1"), repository.refreshCalls)
    }

    @Test
    fun `refresh failure forwards the repository's error without touching content state`() = runTest(dispatcher) {
        repository.nextResult = AppResult.Failure(AppError.Network)
        val viewModel = TodayViewModel(repository, currentUser("user-1"), analyticsLogger)

        viewModel.effects.test {
            val effect = awaitItem()
            assertTrue(effect is TodayUiEffect.ShowSnackbar)
            assertEquals(AppError.Network, (effect as TodayUiEffect.ShowSnackbar).error)
            cancelAndConsumeRemainingEvents()
        }

        assertNull(repository.cachedNow())
        assertNull(viewModel.state.value.content)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `Retry event triggers another refresh`() = runTest(dispatcher) {
        repository.nextResult = AppResult.Failure(AppError.Unknown("first"))
        val viewModel = TodayViewModel(repository, currentUser("user-1"), analyticsLogger)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, repository.refreshCalls.size)

        repository.nextResult = AppResult.Success(sampleContent)
        viewModel.onEvent(TodayUiEvent.Retry)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("user-1", "user-1"), repository.refreshCalls)
        assertEquals(sampleContent, viewModel.state.value.content)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `missing uid emits an Unauthorized snackbar and never touches the repository`() = runTest(dispatcher) {
        val viewModel = TodayViewModel(repository, currentUser(null), analyticsLogger)

        viewModel.effects.test {
            val effect = awaitItem()
            assertEquals(TodayUiEffect.ShowSnackbar(AppError.Unauthorized), effect)
            cancelAndConsumeRemainingEvents()
        }

        assertTrue(repository.refreshCalls.isEmpty())
        assertFalse(viewModel.state.value.isLoading)
    }

    private fun currentUser(uid: String?): CurrentUserProvider = object : CurrentUserProvider {
        override val currentUserId: String? = uid
    }
}
