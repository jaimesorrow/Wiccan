package com.jaimesorrow.wiccan.ui.archive

import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.jaimesorrow.wiccan.domain.model.ContentType
import com.jaimesorrow.wiccan.domain.model.HistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveGrimoireSearchTest {
    private val entry = HistoryEntry(
        contentId = "content-1",
        title = "Candle of Clear Intent",
        type = ContentType.LESSON,
        tradition = "Contemporary Wicca",
        category = ContentCategory.MOON_AND_SEASONAL,
        deliveredDate = "2026-09-10",
    )

    @Test
    fun `search matches localized category label instead of enum name`() {
        val results = listOf(entry).filteredByGrimoireSearch(
            rawQuery = "luna y estaciones",
            localizedCategory = { category ->
                when (category) {
                    ContentCategory.MOON_AND_SEASONAL -> "Luna y estaciones"
                    else -> category.name
                }
            },
            localizedType = { "Lección" },
        )

        assertEquals(listOf(entry), results)
    }

    @Test
    fun `search misses when only the English enum label would match`() {
        val results = listOf(entry).filteredByGrimoireSearch(
            rawQuery = "moon and seasonal",
            localizedCategory = { _ -> "Luna y estaciones" },
            localizedType = { "Lección" },
        )

        assertEquals(emptyList<HistoryEntry>(), results)
    }
}