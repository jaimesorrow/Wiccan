package com.nyxwell.app.ui.archive

import app.cash.turbine.test
import com.nyxwell.app.core.error.AppError
import com.nyxwell.app.core.result.AppResult
import com.nyxwell.app.domain.model.Content
import com.nyxwell.app.domain.model.ContentStatus
import com.nyxwell.app.domain.model.ContentTier
import com.nyxwell.app.domain.model.ContentType
import com.nyxwell.app.domain.model.HistoryEntry
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
class ArchiveViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeArchiveRepository
    private lateinit var viewModel: ArchiveViewModel

    private val entry = HistoryEntry(
        contentId = "content-1",
        title = "Candle of Clear Intent",
        type = ContentType.SPELL,
        tradition = "European folk magic",
        deliveredDate = "2026-09-10",
    )

    private val content = Content(
        id = "content-1",
        type = ContentType.SPELL,
        tier = ContentTier.FREE,
        tradition = "European folk magic",
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
        viewModel = ArchiveViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts loading then reflects the repository's history stream`() = runTest(dispatcher) {
        assertTrue(viewModel.state.value.isLoading)

        repository.emitHistory(listOf(entry))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(entry), viewModel.state.value.entries)
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun `an empty history stream leaves entries empty, not loading`() = runTest(dispatcher) {
        repository.emitHistory(emptyList())
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.entries.isEmpty())
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun `EntryClicked loads and sets the selected content on success`() = runTest(dispatcher) {
        repository.contentResults["content-1"] = AppResult.Success(content)

        viewModel.onEvent(ArchiveUiEvent.EntryClicked("content-1"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(content, viewModel.state.value.selectedContent)
        assertEquals(false, viewModel.state.value.isDetailLoading)
    }

    @Test
    fun `DismissDetail clears the selected content`() = runTest(dispatcher) {
        repository.contentResults["content-1"] = AppResult.Success(content)
        viewModel.onEvent(ArchiveUiEvent.EntryClicked("content-1"))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ArchiveUiEvent.DismissDetail)

        assertNull(viewModel.state.value.selectedContent)
    }

    @Test
    fun `a failed detail fetch forwards the repository's error and leaves content unset`() = runTest(dispatcher) {
        repository.contentResults["missing"] = AppResult.Failure(AppError.NotFound)

        viewModel.effects.test {
            viewModel.onEvent(ArchiveUiEvent.EntryClicked("missing"))
            val effect = awaitItem()
            assertTrue(effect is ArchiveUiEffect.ShowSnackbar)
            assertEquals(AppError.NotFound, (effect as ArchiveUiEffect.ShowSnackbar).error)
            cancelAndConsumeRemainingEvents()
        }
        assertNull(viewModel.state.value.selectedContent)
    }
}
