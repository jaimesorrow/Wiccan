package com.jaimesorrow.wiccan.data.tides

import com.jaimesorrow.wiccan.domain.model.TidePrediction
import com.jaimesorrow.wiccan.domain.model.TideStation
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal object NoaaTideParser {
    fun stations(json: String): List<TideStation> = objectsInArray(json, "stations").mapNotNull { value ->
        val id = string(value, "id") ?: return@mapNotNull null
        val name = string(value, "name") ?: return@mapNotNull null
        val latitude = number(value, "lat") ?: return@mapNotNull null
        val longitude = number(value, "lng") ?: return@mapNotNull null
        TideStation(id, name, latitude, longitude)
    }.toList()

    fun predictions(json: String, zoneId: ZoneId): List<TidePrediction> =
        objectsInArray(json, "predictions").mapNotNull { value ->
            val time = string(value, "t") ?: return@mapNotNull null
            val height = string(value, "v")?.toDoubleOrNull() ?: return@mapNotNull null
            val type = string(value, "type") ?: return@mapNotNull null
            TidePrediction(
                time = LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).atZone(zoneId),
                heightMeters = height,
                high = type.equals("H", ignoreCase = true),
            )
        }

    /** Splits one named JSON array without depending on Android's runtime-only JSON classes. */
    private fun objectsInArray(json: String, key: String): List<String> {
        val keyIndex = json.indexOf("\"$key\"")
        val arrayStart = if (keyIndex >= 0) json.indexOf('[', keyIndex) else -1
        if (arrayStart < 0) return emptyList()
        val objects = mutableListOf<String>()
        var objectStart = -1
        var objectDepth = 0
        var inString = false
        var escaped = false
        for (index in (arrayStart + 1) until json.length) {
            val character = json[index]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (character == '\\') {
                    escaped = true
                } else if (character == '"') {
                    inString = false
                }
                continue
            }
            when (character) {
                '"' -> inString = true
                '{' -> {
                    if (objectDepth == 0) objectStart = index
                    objectDepth++
                }
                '}' -> {
                    objectDepth--
                    if (objectDepth == 0 && objectStart >= 0) {
                        objects += json.substring(objectStart, index + 1)
                        objectStart = -1
                    }
                }
                ']' -> if (objectDepth == 0) return objects
            }
        }
        return objects
    }

    private fun string(value: String, key: String): String? =
        Regex("""\"${Regex.escape(key)}\"\s*:\s*\"([^\"]*)\"""").find(value)?.groupValues?.get(1)

    private fun number(value: String, key: String): Double? =
        Regex("""\"${Regex.escape(key)}\"\s*:\s*\"?(-?\d+(?:\.\d+)?)\"?""")
            .find(value)?.groupValues?.get(1)?.toDoubleOrNull()
}
