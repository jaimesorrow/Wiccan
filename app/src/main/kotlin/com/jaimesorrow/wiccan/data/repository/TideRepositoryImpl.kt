package com.jaimesorrow.wiccan.data.repository

import android.content.Context
import androidx.core.content.edit
import com.jaimesorrow.wiccan.data.location.roundedForStationLookup
import com.jaimesorrow.wiccan.data.tides.NoaaTideParser
import com.jaimesorrow.wiccan.data.tides.TideDataSource
import com.jaimesorrow.wiccan.domain.model.CoastalTide
import com.jaimesorrow.wiccan.domain.model.TideStation
import com.jaimesorrow.wiccan.domain.repository.TideRepository
import com.jaimesorrow.wiccan.sky.GeoCoordinate
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class NoNearbyTideStationException : Exception("No NOAA tide station within range")

@Singleton
class TideRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val dataSource: TideDataSource,
) : TideRepository {
    private val cache = context.getSharedPreferences("noaa_tide_cache", Context.MODE_PRIVATE)
    private val clock: Clock = Clock.systemDefaultZone()

    override suspend fun tidesNear(coordinate: GeoCoordinate, date: LocalDate): Result<CoastalTide> = runCatching {
        val lookupCoordinate = coordinate.roundedForStationLookup()
        val stationJson = cachedStations()
        val station = nearestStation(NoaaTideParser.stations(stationJson), lookupCoordinate)
            ?: error("No NOAA tide station is available")
        val distanceKm = distanceKm(lookupCoordinate, station)
        if (distanceKm > MAX_STATION_DISTANCE_KM) {
            throw NoNearbyTideStationException()
        }
        val key = "predictions_${station.id}_$date"
        val cached = cache.getString(key, null)
        var fromCache = false
        val raw = try {
            dataSource.predictionsJson(station.id, date.format(DateTimeFormatter.BASIC_ISO_DATE)).also {
                cache.edit {
                    putString(key, it)
                    putLong("${key}_at", clock.millis())
                }
            }
        } catch (error: Exception) {
            fromCache = true
            cached ?: throw error
        }
        val deviceZone = ZoneId.systemDefault()
        val parsedPredictions = NoaaTideParser.predictions(raw, ZoneOffset.UTC).map { prediction ->
            prediction.copy(time = prediction.time.withZoneSameInstant(deviceZone))
        }
        val predictions = parsedPredictions.filter { it.time.toInstant() >= clock.instant() }
            .ifEmpty { parsedPredictions }
        check(predictions.isNotEmpty()) { "NOAA returned no tide predictions" }
        CoastalTide(station, predictions, fromCache = fromCache)
    }

    private suspend fun cachedStations(): String {
        val cached = cache.getString(STATIONS_KEY, null)
        val age = clock.millis() - cache.getLong(STATIONS_AT_KEY, 0L)
        if (cached != null && age < STATION_CACHE_MILLIS) return cached
        return try {
            dataSource.stationsJson().also { raw ->
                cache.edit {
                    putString(STATIONS_KEY, raw)
                    putLong(STATIONS_AT_KEY, clock.millis())
                }
            }
        } catch (error: Exception) {
            cached ?: throw error
        }
    }

    internal companion object {
        private const val STATIONS_KEY = "stations"
        private const val STATIONS_AT_KEY = "stations_at"
        private const val STATION_CACHE_MILLIS = 30L * 24L * 60L * 60L * 1_000L
        const val MAX_STATION_DISTANCE_KM = 80.0

        fun nearestStation(stations: List<TideStation>, coordinate: GeoCoordinate): TideStation? =
            stations.minByOrNull { station -> distanceKm(coordinate, station) }

        fun distanceKm(origin: GeoCoordinate, station: TideStation): Double {
            val latitudeDelta = Math.toRadians(station.latitude - origin.latitudeDegrees)
            val longitudeDelta = Math.toRadians(station.longitude - origin.longitudeDegrees)
            val a = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
                cos(Math.toRadians(origin.latitudeDegrees)) * cos(Math.toRadians(station.latitude)) *
                sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
            return 12_742.0 * asin(sqrt(a))
        }
    }
}
