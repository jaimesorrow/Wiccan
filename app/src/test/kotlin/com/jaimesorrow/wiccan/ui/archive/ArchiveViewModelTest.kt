package com.jaimesorrow.wiccan.ui.archive

import app.cash.turbine.test
import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.ContentType
import com.jaimesorrow.wiccan.domain.model.HistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
        category = ContentCategory.MOON_AND_SEASONAL,
        deliveredDate = "2026-09-10"
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
    fun `EntryClicked emits a NavigateToDetail effect for that content id`() = runTest(dispatcher) {
        viewModel.effects.test {
            viewModel.onEvent(ArchiveUiEvent.EntryClicked("content-1"))
            val effect = awaitItem()
            assertTrue(effect is ArchiveUiEffect.NavigateToDetail)
            assertEquals("content-1", (effect as ArchiveUiEffect.NavigateToDetail).contentId)
            cancelAndConsumeRemainingEvents()
        }
    }
}
