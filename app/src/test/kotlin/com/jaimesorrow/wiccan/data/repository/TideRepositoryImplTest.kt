package com.jaimesorrow.wiccan.data.repository

import com.jaimesorrow.wiccan.domain.model.TideStation
import com.jaimesorrow.wiccan.sky.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Test

class TideRepositoryImplTest {
    @Test
    fun `nearest station uses geographic distance`() {
        val anchorage = TideStation("1", "Anchorage", 61.2383, -149.89)
        val seward = TideStation("2", "Seward", 60.12, -149.43)

        val nearest = TideRepositoryImpl.nearestStation(
            listOf(seward, anchorage),
            GeoCoordinate(61.2181, -149.9003),
        )

        assertEquals(anchorage, nearest)
    }
}
