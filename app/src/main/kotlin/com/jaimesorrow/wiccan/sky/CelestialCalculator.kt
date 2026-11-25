package com.jaimesorrow.wiccan.sky

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoCoordinate(val latitudeDegrees: Double, val longitudeDegrees: Double)

enum class ClassicalPlanet(val symbol: String) {
    SATURN("♄"),
    JUPITER("♃"),
    MARS("♂"),
    SUN("☉"),
    VENUS("♀"),
    MERCURY("☿"),
    MOON("☽"),
}

data class MoonObservation(
    val name: String,
    val elongationDegrees: Double,
    val illumination: Double,
    val longitudeDegrees: Double,
    val zodiacSign: String,
    val trackIndex: Int,
    val timingGuidance: String,
)

data class Daylight(val sunrise: ZonedDateTime, val sunset: ZonedDateTime)

data class PlanetaryHour(
    val planet: ClassicalPlanet,
    val hourIndex: Int,
    val dayHour: Boolean,
)

data class VoidOfCourse(
    val active: Boolean,
    val until: Instant,
    val currentSign: String,
    val nextSign: String,
)

data class SabbatObservance(
    val name: String,
    val date: LocalDate,
    val historicalSignificance: String,
    val observedToday: Boolean,
)

private const val DEG = PI / 180.0
private val SIGNS = listOf(
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces",
)
private val CHALDEAN = listOf(
    ClassicalPlanet.SATURN,
    ClassicalPlanet.JUPITER,
    ClassicalPlanet.MARS,
    ClassicalPlanet.SUN,
    ClassicalPlanet.VENUS,
    ClassicalPlanet.MERCURY,
    ClassicalPlanet.MOON,
)

private data class MoonTerm(val d: Int, val m: Int, val mp: Int, val f: Int, val coeff: Int)

/** Largest periodic terms from Meeus, Astronomical Algorithms, Table 47.A. */
private val MOON_TERMS = listOf(
    MoonTerm(0, 0, 1, 0, 6288774),
    MoonTerm(2, 0, -1, 0, 1274027),
    MoonTerm(2, 0, 0, 0, 658314),
    MoonTerm(0, 0, 2, 0, 213618),
    MoonTerm(0, 1, 0, 0, -185116),
    MoonTerm(0, 0, 0, 2, -114332),
    MoonTerm(2, 0, -2, 0, 58793),
    MoonTerm(2, -1, -1, 0, 57066),
    MoonTerm(2, 0, 1, 0, 53322),
    MoonTerm(2, -1, 0, 0, 45758),
    MoonTerm(0, 1, -1, 0, -40923),
    MoonTerm(1, 0, 0, 0, -34720),
    MoonTerm(0, 1, 1, 0, -30383),
    MoonTerm(2, 0, 0, -2, 15327),
    MoonTerm(0, 0, 1, 2, -12528),
    MoonTerm(0, 0, 1, -2, 10980),
    MoonTerm(4, 0, -1, 0, 10675),
    MoonTerm(0, 0, 3, 0, 10034),
    MoonTerm(4, 0, -2, 0, 8548),
    MoonTerm(2, 1, -1, 0, -7888),
    MoonTerm(2, 1, 0, 0, -6766),
    MoonTerm(1, 0, -1, 0, -5163),
    MoonTerm(1, 1, 0, 0, 4987),
    MoonTerm(2, -1, 1, 0, 4036),
    MoonTerm(2, 0, 2, 0, 3994),
    MoonTerm(4, 0, 0, 0, 3861),
    MoonTerm(2, 0, -3, 0, 3665),
    MoonTerm(0, 1, -2, 0, -2689),
    MoonTerm(2, 0, -1, 2, -2602),
    MoonTerm(2, -1, -2, 0, 2390),
    MoonTerm(1, 0, 1, 0, -2348),
    MoonTerm(2, -2, 0, 0, 2236),
    MoonTerm(0, 1, 2, 0, -2120),
    MoonTerm(0, 2, 0, 0, -2069),
    MoonTerm(2, -2, -1, 0, 2048),
    MoonTerm(2, 0, 1, -2, -1773),
    MoonTerm(2, 0, 0, 2, -1595),
    MoonTerm(4, 0, -1, -2, 1215),
    MoonTerm(0, 0, 2, 2, -1110),
)

private data class Orbit(
    val n0: Double,
    val n1: Double,
    val i0: Double,
    val i1: Double,
    val w0: Double,
    val w1: Double,
    val a: Double,
    val e0: Double,
    val e1: Double,
    val m0: Double,
    val m1: Double,
)

/** Low-precision elements from Paul Schlyter, sufficient for sign and aspect. */
private val EARTH = Orbit(0.0, 0.0, 0.0, 0.0, 282.9404, 4.70935E-5, 1.0, 0.016709, -1.151E-9, 356.0470, 0.9856002585)
private val ORBITS = mapOf(
    ClassicalPlanet.MERCURY to Orbit(
        48.3313, 3.24587E-5, 7.0047, 5.00E-8, 29.1241, 1.01444E-5,
        0.387098, 0.205635, 5.59E-10, 168.6562, 4.0923344368,
    ),
    ClassicalPlanet.VENUS to Orbit(
        76.6799, 2.46590E-5, 3.3946, 2.75E-8, 54.8910, 1.38374E-5,
        0.723330, 0.006773, -1.302E-9, 48.0052, 1.6021302244,
    ),
    ClassicalPlanet.MARS to Orbit(
        49.5574, 2.11081E-5, 1.8497, -1.78E-8, 286.5016, 2.92961E-5,
        1.523688, 0.093405, 2.516E-9, 18.6021, 0.5240207766,
    ),
    ClassicalPlanet.JUPITER to Orbit(
        100.4542, 2.76854E-5, 1.3030, -1.557E-7, 273.8777, 1.64505E-5,
        5.20256, 0.048498, 4.469E-9, 19.8950, 0.0830853001,
    ),
    ClassicalPlanet.SATURN to Orbit(
        113.6634, 2.38980E-5, 2.4886, -1.081E-7, 339.3939, 2.97661E-5,
        9.55475, 0.055546, -9.499E-9, 316.9670, 0.0334442282,
    ),
)

object CelestialCalculator {
    fun julianDay(instant: Instant): Double = instant.toEpochMilli() / 86_400_000.0 + 2_440_587.5

    fun sunApparentLongitude(jd: Double): Double {
        val t = (jd - 2_451_545.0) / 36_525.0
        val mean = wrap360(280.46646 + 36_000.76983 * t + 0.0003032 * t * t)
        val anomaly = wrap360(357.52911 + 35_999.05029 * t - 0.0001537 * t * t)
        val center = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sinD(anomaly) +
            (0.019993 - 0.000101 * t) * sinD(2 * anomaly) +
            0.000289 * sinD(3 * anomaly)
        val omega = 125.04 - 1934.136 * t
        return wrap360(mean + center - 0.00569 - 0.00478 * sinD(omega))
    }

    fun moonLongitude(jd: Double): Double {
        val t = (jd - 2_451_545.0) / 36_525.0
        val lp = wrap360(
            218.3164477 + 481_267.88123421 * t - 0.0015786 * t * t +
                t * t * t / 538_841.0 - t * t * t * t / 65_194_000.0,
        )
        val d = wrap360(
            297.8501921 + 445_267.1114034 * t - 0.0018819 * t * t +
                t * t * t / 545_868.0 - t * t * t * t / 113_065_000.0,
        )
        val m = wrap360(357.5291092 + 35_999.0502909 * t - 0.0001536 * t * t + t * t * t / 24_490_000.0)
        val mp = wrap360(
            134.9633964 + 477_198.8675055 * t + 0.0087414 * t * t +
                t * t * t / 69_699.0 - t * t * t * t / 14_712_000.0,
        )
        val f = wrap360(
            93.2720950 + 483_202.0175233 * t - 0.0036539 * t * t -
                t * t * t / 3_526_000.0 + t * t * t * t / 863_310_000.0,
        )
        val sum = MOON_TERMS.sumOf { term ->
            term.coeff * sinD(term.d * d + term.m * m + term.mp * mp + term.f * f)
        }
        return wrap360(lp + sum / 1_000_000.0)
    }

    fun zodiacSign(longitudeDegrees: Double): String = SIGNS[(wrap360(longitudeDegrees) / 30.0).toInt().coerceIn(0, 11)]

    fun moon(instant: Instant): MoonObservation {
        val jd = julianDay(instant)
        val elongation = wrap360(moonLongitude(jd) - sunApparentLongitude(jd))
        val name = when {
            elongation < 22.5 || elongation >= 337.5 -> "New Moon"
            elongation < 67.5 -> "Waxing Crescent"
            elongation < 112.5 -> "First Quarter"
            elongation < 157.5 -> "Waxing Gibbous"
            elongation < 202.5 -> "Full Moon"
            elongation < 247.5 -> "Waning Gibbous"
            elongation < 292.5 -> "Last Quarter"
            else -> "Waning Crescent"
        }
        val guidance = when (name) {
            "New Moon" -> "A quiet threshold for setting one intention and beginning gently."
            "Waxing Crescent" -> "Energy is gathering. Take a small committed step toward what you are growing."
            "First Quarter" -> "A working hour for resolve, adjustment, and hands-on effort."
            "Waxing Gibbous" -> "Refine what is already moving and tend what still needs care."
            "Full Moon" -> "Illumination favors gratitude, recognition, and a wider view of the work."
            "Waning Gibbous" -> "Share what you learned and make room to integrate it."
            "Last Quarter" -> "A clearing phase for boundaries, discernment, and gentle endings."
            else -> "Rest, restore, and listen before the next beginning."
        }
        val track = listOf(
            "New Moon",
            "Waxing Crescent",
            "First Quarter",
            "Waxing Gibbous",
            "Full Moon",
            "Waning Gibbous",
            "Last Quarter",
            "Waning Crescent",
        ).indexOf(name)
        return MoonObservation(
            name = name,
            elongationDegrees = elongation,
            illumination = (1.0 - cosD(elongation)) / 2.0,
            longitudeDegrees = moonLongitude(jd),
            zodiacSign = zodiacSign(moonLongitude(jd)),
            trackIndex = track,
            timingGuidance = guidance,
        )
    }

    fun geocentricLongitude(planet: ClassicalPlanet, jd: Double): Double = when (planet) {
        ClassicalPlanet.SUN -> sunApparentLongitude(jd)
        ClassicalPlanet.MOON -> moonLongitude(jd)
        else -> {
            val day = jd - 2_451_543.5
            val body = heliocentric(ORBITS.getValue(planet), day)
            val earth = heliocentric(EARTH, day)
            wrap360(atan2(body.second - earth.second, body.first - earth.first) / DEG)
        }
    }

    /** Sunrise and sunset for a local calendar date. Null in polar day or night. */
    fun daylight(date: LocalDate, zone: ZoneId, geo: GeoCoordinate): Daylight? {
        val noon = date.atTime(12, 0).atZone(ZoneId.of("UTC")).toInstant()
        val n = julianDay(noon) - 2_451_545.0 + 0.0008
        val jStar = n - geo.longitudeDegrees / 360.0
        val anomaly = wrap360(357.5291 + 0.98560028 * jStar)
        val center = 1.9148 * sinD(anomaly) + 0.0200 * sinD(2 * anomaly) + 0.0003 * sinD(3 * anomaly)
        val lambda = wrap360(anomaly + center + 180.0 + 102.9372)
        val transit = 2_451_545.0 + jStar + 0.0053 * sinD(anomaly) - 0.0069 * sinD(2 * lambda)
        val declination = asin(sinD(lambda) * sinD(23.4397))
        val latitude = geo.latitudeDegrees * DEG
        val denominator = cos(latitude) * cos(declination)
        if (abs(denominator) < 1e-9) return null
        val cosHour = (sinD(-0.833) - sin(latitude) * sin(declination)) / denominator
        if (cosHour < -1.0 || cosHour > 1.0) return null
        val hourAngle = acos(cosHour) / DEG
        return Daylight(
            sunrise = instantFromJulian(transit - hourAngle / 360.0).atZone(zone),
            sunset = instantFromJulian(transit + hourAngle / 360.0).atZone(zone),
        )
    }

    /**
     * Chaldean planetary hour from local sunrise and sunset.
     * Returns null without a location or when the sun does not rise and set.
     */
    fun planetaryHour(instant: Instant, zone: ZoneId, geo: GeoCoordinate?): PlanetaryHour? {
        if (geo == null) return null
        val date = instant.atZone(zone).toLocalDate()
        val today = daylight(date, zone, geo) ?: return null
        val yesterday = daylight(date.minusDays(1), zone, geo) ?: return null
        val tomorrow = daylight(date.plusDays(1), zone, geo) ?: return null
        val moment = instant.atZone(zone)
        val span = when {
            moment < today.sunrise -> NightSpan(yesterday.sunset, today.sunrise, date.minusDays(1))
            moment < today.sunset -> DaySpan(today.sunrise, today.sunset, date)
            else -> NightSpan(today.sunset, tomorrow.sunrise, date)
        }
        val length = Duration.between(span.start, span.end).toMillis().toDouble()
        if (length <= 0.0) return null
        val elapsed = Duration.between(span.start, moment).toMillis().toDouble()
        val indexInSpan = ((elapsed / length) * 12.0).toInt().coerceIn(0, 11)
        val hourIndex = span.firstHour + indexInSpan
        val ruler = dayRuler(span.rulerDate.dayOfWeek)
        val planet = CHALDEAN[(CHALDEAN.indexOf(ruler) + hourIndex) % CHALDEAN.size]
        return PlanetaryHour(planet, hourIndex, span is DaySpan)
    }

    /**
     * Void of course: no major aspect to a classical planet remains before the
     * Moon leaves its current sign. Sampled every 15 minutes, 1° orb.
     */
    fun voidOfCourse(instant: Instant): VoidOfCourse {
        val start = julianDay(instant)
        val startLongitude = moonLongitude(start)
        val signIndex = (startLongitude / 30.0).toInt().coerceIn(0, 11)
        var active = true
        var exit = start + 2.5
        for (step in 1..288) {
            val jd = start + step * (15.0 / 1_440.0)
            val moon = moonLongitude(jd)
            if ((moon / 30.0).toInt().mod(12) != signIndex) {
                exit = jd
                break
            }
            val applying = listOf(
                ClassicalPlanet.SUN,
                ClassicalPlanet.MERCURY,
                ClassicalPlanet.VENUS,
                ClassicalPlanet.MARS,
                ClassicalPlanet.JUPITER,
                ClassicalPlanet.SATURN,
            ).any { planet -> aspectOffset(moon, geocentricLongitude(planet, jd)) <= 1.0 }
            if (applying) active = false
        }
        return VoidOfCourse(
            active = active,
            until = instantFromJulian(exit),
            currentSign = SIGNS[signIndex],
            nextSign = SIGNS[(signIndex + 1) % SIGNS.size],
        )
    }

    fun sabbats(year: Int): List<SabbatObservance> = listOf(
        fixed(
            "Imbolc",
            year,
            2,
            1,
            "A Gaelic seasonal festival, later kept in Wicca, marking the first stirrings of spring and the return of light.",
        ),
        solar(
            "Ostara",
            year,
            0.0,
            "The spring equinox, when day and night balance and Wiccan practice turns toward the growing half of the year.",
        ),
        fixed(
            "Beltane",
            year,
            5,
            1,
            "A Gaelic fire festival opening the summer half of the year, associated with vitality, protection, and gathering.",
        ),
        solar("Litha", year, 90.0, "The summer solstice, the longest day, kept as the sun at its height before the light begins to turn."),
        fixed("Lughnasadh", year, 8, 1, "A Gaelic first-harvest festival associated with Lugh, skill, and the earliest grain."),
        solar("Mabon", year, 180.0, "The autumn equinox, a modern Wiccan harvest observance of balance, thanks, and the waning light."),
        fixed("Samhain", year, 10, 31, "A Gaelic festival of summer's end and winter's beginning, widely kept as a night of remembrance."),
        solar(
            "Yule",
            year,
            270.0,
            "The winter solstice, the longest night, when the sun reaches its turning and the light begins its return.",
        ),
    )

    fun sabbatFor(date: LocalDate): SabbatObservance {
        val upcoming = (sabbats(date.year) + sabbats(date.year + 1)).filter { !it.date.isBefore(date) }.minBy { it.date }
        return if (upcoming.date == date) upcoming.copy(observedToday = true) else upcoming
    }

    fun aspectOffset(first: Double, second: Double): Double {
        var separation = abs(wrap360(first) - wrap360(second))
        if (separation > 180.0) separation = 360.0 - separation
        return doubleArrayOf(0.0, 60.0, 90.0, 120.0, 180.0).minOf { abs(separation - it) }
    }

    private fun dayRuler(day: java.time.DayOfWeek): ClassicalPlanet = when (day) {
        java.time.DayOfWeek.SUNDAY -> ClassicalPlanet.SUN
        java.time.DayOfWeek.MONDAY -> ClassicalPlanet.MOON
        java.time.DayOfWeek.TUESDAY -> ClassicalPlanet.MARS
        java.time.DayOfWeek.WEDNESDAY -> ClassicalPlanet.MERCURY
        java.time.DayOfWeek.THURSDAY -> ClassicalPlanet.JUPITER
        java.time.DayOfWeek.FRIDAY -> ClassicalPlanet.VENUS
        java.time.DayOfWeek.SATURDAY -> ClassicalPlanet.SATURN
    }

    private fun fixed(name: String, year: Int, month: Int, day: Int, significance: String) =
        SabbatObservance(name, LocalDate.of(year, month, day), significance, false)

    private fun solar(name: String, year: Int, longitude: Double, significance: String): SabbatObservance {
        val window = when (longitude) {
            0.0 -> LocalDate.of(year, 3, 16)
            90.0 -> LocalDate.of(year, 6, 16)
            180.0 -> LocalDate.of(year, 9, 16)
            else -> LocalDate.of(year, 12, 16)
        }
        val date = (0..12).map { window.plusDays(it.toLong()) }.minBy { candidate ->
            val longitudeNow = sunApparentLongitude(julianDay(candidate.atStartOfDay(ZoneId.of("UTC")).toInstant()))
            angularDistance(longitudeNow, longitude)
        }
        return SabbatObservance(name, date, significance, false)
    }
}

private interface HourSpan {
    val start: ZonedDateTime
    val end: ZonedDateTime
    val rulerDate: LocalDate
    val firstHour: Int
}

private class DaySpan(override val start: ZonedDateTime, override val end: ZonedDateTime, override val rulerDate: LocalDate) : HourSpan {
    override val firstHour: Int = 0
}

private class NightSpan(override val start: ZonedDateTime, override val end: ZonedDateTime, override val rulerDate: LocalDate) : HourSpan {
    override val firstHour: Int = 12
}

private fun heliocentric(orbit: Orbit, day: Double): Triple<Double, Double, Double> {
    val node = orbit.n0 + orbit.n1 * day
    val inclination = orbit.i0 + orbit.i1 * day
    val perihelion = orbit.w0 + orbit.w1 * day
    val eccentricity = orbit.e0 + orbit.e1 * day
    val mean = wrap360(orbit.m0 + orbit.m1 * day) * DEG
    var eccentric = mean
    repeat(8) {
        eccentric -= (eccentric - eccentricity * sin(eccentric) - mean) / (1 - eccentricity * cos(eccentric))
    }
    val xv = orbit.a * (cos(eccentric) - eccentricity)
    val yv = orbit.a * sqrt(1 - eccentricity * eccentricity) * sin(eccentric)
    val angle = atan2(yv, xv) + perihelion * DEG
    val radius = sqrt(xv * xv + yv * yv)
    return Triple(
        radius * (cosD(node) * cos(angle) - sinD(node) * sin(angle) * cosD(inclination)),
        radius * (sinD(node) * cos(angle) + cosD(node) * sin(angle) * cosD(inclination)),
        radius * sin(angle) * sinD(inclination),
    )
}

private fun instantFromJulian(jd: Double): Instant = Instant.ofEpochMilli(((jd - 2_440_587.5) * 86_400_000.0).toLong())

internal fun wrap360(degrees: Double): Double {
    val wrapped = degrees % 360.0
    return if (wrapped < 0) wrapped + 360.0 else wrapped
}

private fun angularDistance(first: Double, second: Double): Double {
    var delta = abs(wrap360(first) - wrap360(second))
    if (delta > 180.0) delta = 360.0 - delta
    return delta
}

private fun sinD(degrees: Double) = sin(degrees * DEG)
private fun cosD(degrees: Double) = cos(degrees * DEG)
