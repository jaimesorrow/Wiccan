package com.wiccan.app.ui.archive

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wiccan.app.core.error.AppError
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.domain.model.Content
import com.wiccan.app.domain.model.ContentCategory
import com.wiccan.app.domain.model.ContentStatus
import com.wiccan.app.domain.model.ContentTier
import com.wiccan.app.domain.model.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeArchiveRepository

    private val content = Content(
        id = "content-1",
        type = ContentType.SPELL,
        tier = ContentTier.FREE,
        tradition = "European folk magic",
        category = ContentCategory.MOON_AND_SEASONAL,
        title = "Candle of Clear Intent",
        intro = "intro",
        body = "body",
        materials = null,
        safetyNote = null,
        tags = emptyList(),
        status = ContentStatus.PUBLISHED,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeArchiveRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(contentId: String) = ArchiveDetailViewModel(
        SavedStateHandle(mapOf(ARCHIVE_DETAIL_CONTENT_ID_ARG to contentId)),
        repository,
    )

    @Test
    fun `loads and exposes the content for the nav-arg content id`() = runTest(dispatcher) {
        repository.contentResults["content-1"] = AppResult.Success(content)

        val viewModel = viewModel("content-1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(content, viewModel.state.value.content)
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun `a failed fetch forwards the repository's error and leaves content unset`() = runTest(dispatcher) {
        repository.contentResults["missing"] = AppResult.Failure(AppError.NotFound)

        val viewModel = viewModel("missing")
        viewModel.effects.test {
            dispatcher.scheduler.advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is ArchiveDetailUiEffect.ShowSnackbar)
            assertEquals(AppError.NotFound, (effect as ArchiveDetailUiEffect.ShowSnackbar).error)
            cancelAndConsumeRemainingEvents()
        }
        assertNull(viewModel.state.value.content)
    }
}
