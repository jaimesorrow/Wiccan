package com.jaimesorrow.wiccan.ui.content

import androidx.test.core.app.ApplicationProvider
import com.jaimesorrow.wiccan.seasonal.SeasonalCalendar
import com.jaimesorrow.wiccan.seasonal.SeasonalWindow
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class SeasonalChipTest {
    private val resources = ApplicationProvider.getApplicationContext<android.app.Application>().resources

    private fun label(date: LocalDate): String? {
        val window = SeasonalCalendar.seasonalWindowFor(date) ?: return null
        return seasonalChipLabel(window, resources)
    }

    @Test
    fun `sabbat takes priority over the moon label`() {
        assertEquals("Samhain", label(LocalDate.of(2026, 10, 31)))
    }

    @Test
    fun `full and new moon copy names a window, not a single night`() {
        assertEquals("Near the Full Moon", label(LocalDate.of(2026, 3, 3)))
        assertEquals("Near the New Moon", label(LocalDate.of(2026, 8, 12)))
        assertNull(label(LocalDate.of(2026, 7, 7)))
        listOf(
            LocalDate.of(2026, 3, 2),
            LocalDate.of(2026, 3, 3),
            LocalDate.of(2026, 3, 4),
        ).forEach { date ->
            val text = label(date)!!
            assertEquals(date.toString(), "Near the Full Moon", text)
            assertFalse(text.contains("Tonight", ignoreCase = true))
        }
    }

    @Test
    fun `moon track and chip share the civil-date phase`() {
        val full = LocalDate.of(2026, 3, 3)
        val fullTrack = SeasonalCalendar.moonTrack(full)
        assertEquals(SeasonalWindow.Moon("full"), SeasonalCalendar.seasonalWindowFor(full))
        assertEquals("Full Moon", fullTrack.name)
        assertEquals(4, fullTrack.trackIndex)

        val newMoon = LocalDate.of(2026, 8, 12)
        val newTrack = SeasonalCalendar.moonTrack(newMoon)
        assertEquals(SeasonalWindow.Moon("new"), SeasonalCalendar.seasonalWindowFor(newMoon))
        assertEquals("New Moon", newTrack.name)
        assertEquals(0, newTrack.trackIndex)

        val ordinary = LocalDate.of(2026, 7, 7)
        val ordinaryTrack = SeasonalCalendar.moonTrack(ordinary)
        assertNull(SeasonalCalendar.seasonalWindowFor(ordinary))
        assertTrue(ordinaryTrack.trackIndex != 0 && ordinaryTrack.trackIndex != 4)
    }
}
