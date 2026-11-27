package com.jaimesorrow.wiccan.data.tides

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface TideDataSource {
    suspend fun stationsJson(): String
    suspend fun predictionsJson(stationId: String, date: String): String
}

class NoaaTideDataSource @Inject constructor() : TideDataSource {
    override suspend fun stationsJson(): String = get(
        "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations.json?type=tidepredictions",
    )

    override suspend fun predictionsJson(stationId: String, date: String): String {
        val encodedStation = URLEncoder.encode(stationId, StandardCharsets.UTF_8)
        return get(
            "https://api.tidesandcurrents.noaa.gov/api/prod/datagetter" +
                "?product=predictions&application=Wiccan&begin_date=$date&range=48" +
                "&datum=MLLW&station=$encodedStation&time_zone=gmt&units=metric&interval=hilo&format=json",
        )
    }

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Wiccan Android; https://wiccan.app")
        }
        try {
            check(connection.responseCode in 200..299) { "NOAA request failed (${connection.responseCode})" }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
