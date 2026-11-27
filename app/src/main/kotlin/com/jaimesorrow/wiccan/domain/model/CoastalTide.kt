package com.jaimesorrow.wiccan.domain.model

import java.time.ZonedDateTime

data class TideStation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

data class TidePrediction(
    val time: ZonedDateTime,
    val heightMeters: Double,
    val high: Boolean,
)

data class CoastalTide(
    val station: TideStation,
    val predictions: List<TidePrediction>,
    val fromCache: Boolean,
)
