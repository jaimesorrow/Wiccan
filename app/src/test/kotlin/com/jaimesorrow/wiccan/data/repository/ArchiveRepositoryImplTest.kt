package com.jaimesorrow.wiccan.data.repository

import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.data.remote.ArchiveFirestoreDataSource
import com.jaimesorrow.wiccan.domain.model.ContentType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveRepositoryImplTest {

    private val dataSource: ArchiveFirestoreDataSource = mockk()
    private val repository = ArchiveRepositoryImpl(dataSource)

    private val spellRow = mapOf<String, Any?>(
        "contentId" to "spell-1",
        "title" to "Candle of Clear Intent",
        "type" to "spell",
        "deliveredDate" to "2026-11-14",
    )

    @Test
    fun `Grimoire history skips an unknown type instead of crashing the listener`() = runTest {
        val ritualRow = spellRow + ("contentId" to "ritual-1") + ("type" to "ritual") + ("deliveredDate" to "2026-11-15")
        every { dataSource.observeHistory() } returns flowOf(AppResult.Success(listOf(ritualRow, spellRow, mapOf("title" to "no id"))))

        val entries = (repository.observeHistory().first() as AppResult.Success).data

        assertEquals(listOf("spell-1"), entries.map { it.contentId })
        assertEquals(ContentType.SPELL, entries.single().type)
    }

    @Test
    fun `opening an unreadable Grimoire entry fails softly`() = runTest {
        coEvery { dataSource.getContentById("ritual-1") } returns mapOf(
            "id" to "ritual-1",
            "type" to "ritual",
            "tier" to "premium",
            "status" to "Publish",
        )

        val result = repository.getContentById("ritual-1")

        assertEquals(AppError.UnsupportedContent, (result as AppResult.Failure).error)
    }

    @Test
    fun `a legacy Publish status still opens in the Grimoire`() = runTest {
        coEvery { dataSource.getContentById("egypt-heka-001") } returns mapOf(
            "id" to "egypt-heka-001",
            "type" to "lesson",
            "tier" to "premium",
            "status" to "Publish",
        )

        val result = repository.getContentById("egypt-heka-001")

        assertEquals("egypt-heka-001", (result as AppResult.Success).data.id)
    }
}
