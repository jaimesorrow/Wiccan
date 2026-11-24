package com.jaimesorrow.wiccan.sky

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CelestialCalculatorTest {
    @Test
    fun `sun longitude at J2000 is near Capricorn`() {
        val longitude = CelestialCalculator.sunApparentLongitude(2_451_545.0)
        assertTrue("longitude was $longitude", longitude in 279.0..282.0)
        assertEquals("Capricorn", CelestialCalculator.zodiacSign(longitude))
    }

    @Test
    fun `Meeus April 1992 moon longitude is Leo within a degree`() {
        val longitude = CelestialCalculator.moonLongitude(2_448_724.5)
        assertTrue("longitude was $longitude", longitude in 132.0..134.5)
        assertEquals("Leo", CelestialCalculator.zodiacSign(longitude))
    }

    @Test
    fun `known new moon is a dark phase and signs advance every thirty degrees`() {
        val moon = CelestialCalculator.moon(Instant.parse("2000-01-06T18:14:00Z"))
        assertTrue(moon.illumination < 0.02)
        assertEquals("New Moon", moon.name)
        assertEquals("Aries", CelestialCalculator.zodiacSign(0.0))
        assertEquals("Taurus", CelestialCalculator.zodiacSign(30.0))
        assertEquals("Scorpio", CelestialCalculator.zodiacSign(215.0))
        assertEquals("Pisces", CelestialCalculator.zodiacSign(359.0))
    }

    @Test
    fun `equator equinox sunrise is near six UTC and Friday's first hour is Venus`() {
        val geo = GeoCoordinate(0.0, 0.0)
        val light = requireNotNull(CelestialCalculator.daylight(LocalDate.of(2026, 3, 20), ZoneOffset.UTC, geo))
        assertTrue(light.sunrise.hour in 5..7)
        val hour = requireNotNull(CelestialCalculator.planetaryHour(light.sunrise.toInstant().plusSeconds(60), ZoneOffset.UTC, geo))
        assertEquals(ClassicalPlanet.VENUS, hour.planet)
        assertTrue(hour.dayHour)
        assertEquals(null, CelestialCalculator.planetaryHour(Instant.parse("2026-03-20T12:00:00Z"), ZoneOffset.UTC, null))
    }

    @Test
    fun `all eight sabbats keep their cross-quarter dates and solar quarters`() {
        val sabbats = CelestialCalculator.sabbats(2026)
        assertEquals(
            listOf("Imbolc", "Ostara", "Beltane", "Litha", "Lughnasadh", "Mabon", "Samhain", "Yule"),
            sabbats.map { it.name },
        )
        assertEquals(LocalDate.of(2026, 2, 1), sabbats[0].date)
        assertEquals(LocalDate.of(2026, 5, 1), sabbats[2].date)
        assertEquals(LocalDate.of(2026, 8, 1), sabbats[4].date)
        assertEquals(LocalDate.of(2026, 10, 31), sabbats[6].date)
        assertTrue(sabbats[1].date.monthValue == 3 && sabbats[1].date.dayOfMonth in 19..22)
        assertTrue(sabbats[3].date.monthValue == 6 && sabbats[3].date.dayOfMonth in 19..22)
        assertTrue(sabbats[5].date.monthValue == 9 && sabbats[5].date.dayOfMonth in 21..24)
        assertTrue(sabbats[7].date.monthValue == 12 && sabbats[7].date.dayOfMonth in 20..23)
        assertTrue(CelestialCalculator.sabbatFor(LocalDate.of(2026, 10, 31)).observedToday)
        assertEquals("Yule", CelestialCalculator.sabbatFor(LocalDate.of(2026, 11, 1)).name)
        assertEquals(false, CelestialCalculator.sabbatFor(LocalDate.of(2026, 11, 1)).observedToday)
    }

    @Test
    fun `aspect offset finds the classical aspects and void of course has an exit`() {
        assertEquals(0.0, CelestialCalculator.aspectOffset(10.0, 70.0), 0.001)
        assertEquals(0.0, CelestialCalculator.aspectOffset(0.0, 180.0), 0.001)
        val voc = CelestialCalculator.voidOfCourse(Instant.parse("2026-06-21T12:00:00Z"))
        assertTrue(voc.until.isAfter(Instant.parse("2026-06-21T12:00:00Z")))
        assertTrue(voc.currentSign.isNotBlank() && voc.nextSign != voc.currentSign)
    }
}
