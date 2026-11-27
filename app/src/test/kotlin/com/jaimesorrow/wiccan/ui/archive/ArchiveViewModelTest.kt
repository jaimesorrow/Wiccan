package com.jaimesorrow.wiccan.ui.archive

import app.cash.turbine.test
import com.jaimesorrow.wiccan.core.error.AppError
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

    @Test
    fun `a history listener failure is surfaced instead of an empty Grimoire`() = runTest(dispatcher) {
        repository.emitHistory(listOf(entry))
        dispatcher.scheduler.advanceUntilIdle()
        repository.emitHistoryError(AppError.Network)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(entry), viewModel.state.value.entries)
        assertEquals(AppError.Network, viewModel.state.value.error)
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun `search filters title category type and tradition and clear restores entries`() = runTest(dispatcher) {
        val herbalEntry = entry.copy(
            contentId = "content-2",
            title = "Lavender Threshold",
            type = ContentType.LESSON,
            tradition = "Contemporary Wicca",
            category = ContentCategory.HERBAL_RITUALS,
        )
        repository.emitHistory(listOf(entry, herbalEntry))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ArchiveUiEvent.SearchQueryChanged("lavender"))
        assertEquals(listOf(herbalEntry), viewModel.state.value.visibleEntries)
        viewModel.onEvent(ArchiveUiEvent.SearchQueryChanged("herbal rituals"))
        assertEquals(listOf(herbalEntry), viewModel.state.value.visibleEntries)
        viewModel.onEvent(ArchiveUiEvent.SearchQueryChanged("spell"))
        assertEquals(listOf(entry), viewModel.state.value.visibleEntries)
        viewModel.onEvent(ArchiveUiEvent.SearchQueryChanged("contemporary"))
        assertEquals(listOf(herbalEntry), viewModel.state.value.visibleEntries)

        viewModel.onEvent(ArchiveUiEvent.SearchCleared)
        assertEquals(listOf(entry, herbalEntry), viewModel.state.value.visibleEntries)
        assertEquals("", viewModel.state.value.searchQuery)
    }

    @Test
    fun `etymology disclosure toggles without changing archive state`() {
        assertEquals(false, viewModel.state.value.isEtymologyExpanded)
        viewModel.onEvent(ArchiveUiEvent.EtymologyToggled)
        assertEquals(true, viewModel.state.value.isEtymologyExpanded)
        viewModel.onEvent(ArchiveUiEvent.EtymologyToggled)
        assertEquals(false, viewModel.state.value.isEtymologyExpanded)
    }
}
