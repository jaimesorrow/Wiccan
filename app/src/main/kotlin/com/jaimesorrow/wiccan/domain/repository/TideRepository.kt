package com.jaimesorrow.wiccan.domain.repository

import com.jaimesorrow.wiccan.domain.model.CoastalTide
import com.jaimesorrow.wiccan.sky.GeoCoordinate
import java.time.LocalDate

interface TideRepository {
    suspend fun tidesNear(coordinate: GeoCoordinate, date: LocalDate): Result<CoastalTide>
}
