package com.jaimesorrow.wiccan.data.tides

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoaaTideParserTest {
    @Test
    fun `parses NOAA stations`() {
        val json = """
            {"stations":[{"id":"9455920","name":"Anchorage","details":{"self":"url"},
            "lat":61.2383,"lng":-149.8900}]}
        """.trimIndent()

        val station = NoaaTideParser.stations(json).single()

        assertEquals("9455920", station.id)
        assertEquals("Anchorage", station.name)
        assertEquals(61.2383, station.latitude, 0.0001)
    }

    @Test
    fun `parses NOAA high and low predictions`() {
        val json = """
            {"predictions":[{"t":"2026-11-27 05:12","v":"8.214","type":"H"},
            {"t":"2026-11-27 11:38","v":"0.451","type":"L"}]}
        """.trimIndent()

        val predictions = NoaaTideParser.predictions(json, ZoneId.of("America/Anchorage"))

        assertEquals(2, predictions.size)
        assertTrue(predictions.first().high)
        assertFalse(predictions.last().high)
        assertEquals(8.214, predictions.first().heightMeters, 0.0001)
    }
}
