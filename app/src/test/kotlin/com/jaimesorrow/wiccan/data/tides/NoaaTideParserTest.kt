package com.jaimesorrow.wiccan.data.tides

import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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

    @Test
    fun `GMT predictions convert to different local clocks on the device`() {
        val json = """
            {"predictions":[{"t":"2026-06-15 18:00","v":"1.0","type":"H"}]}
        """.trimIndent()
        val utc = NoaaTideParser.predictions(json, ZoneOffset.UTC).single()

        val eastern = utc.time.withZoneSameInstant(ZoneId.of("America/New_York"))
        val alaska = utc.time.withZoneSameInstant(ZoneId.of("America/Anchorage"))

        assertNotEquals(eastern.toLocalTime(), alaska.toLocalTime())
    }

    @Test
    fun `station-local NOAA clock must not be read as the device zone`() {
        val json = """
            {"predictions":[{"t":"2026-06-15 14:00","v":"2.4","type":"H"}]}
        """.trimIndent()
        val stationZone = ZoneId.of("America/New_York")
        val deviceMisparse = ZoneId.of("America/Anchorage")

        val atStation = NoaaTideParser.predictions(json, stationZone).single()
        val misread = NoaaTideParser.predictions(json, deviceMisparse).single()

        assertEquals(14, atStation.time.hour)
        assertNotEquals(atStation.time.toInstant(), misread.time.toInstant())
    }
}
