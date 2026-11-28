package com.jaimesorrow.wiccan.ui.today

import com.jaimesorrow.wiccan.data.repository.NoNearbyTideStationException
import com.jaimesorrow.wiccan.domain.model.CoastalTide
import com.jaimesorrow.wiccan.domain.model.TideHeightUnit
import com.jaimesorrow.wiccan.domain.model.TidePrediction
import com.jaimesorrow.wiccan.domain.model.TideStation
import com.jaimesorrow.wiccan.domain.repository.TideRepository
import com.jaimesorrow.wiccan.sky.GeoCoordinate
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeTideViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `null coordinate keeps needs location`() = runTest {
        val viewModel = HomeTideViewModel(FakeTideRepository())
        viewModel.load(null)
        assertEquals(TideUiState.NeedsLocation, viewModel.state.value)
    }

    @Test
    fun `successful load becomes available`() = runTest {
        val tide = sampleTide()
        val repository = FakeTideRepository(Result.success(tide))
        val viewModel = HomeTideViewModel(repository)
        viewModel.load(GeoCoordinate(61.2, -149.9))
        assertEquals(TideUiState.Available(tide), viewModel.state.value)
    }

    @Test
    fun `no nearby station maps to dedicated state`() = runTest {
        val repository = FakeTideRepository(
            Result.failure(NoNearbyTideStationException()),
        )
        val viewModel = HomeTideViewModel(repository)
        viewModel.load(GeoCoordinate(39.7, -104.9))
        assertEquals(TideUiState.NoNearbyStation, viewModel.state.value)
    }

    @Test
    fun `other failures become unavailable`() = runTest {
        val repository = FakeTideRepository(Result.failure(IllegalStateException("offline")))
        val viewModel = HomeTideViewModel(repository)
        viewModel.load(GeoCoordinate(61.2, -149.9))
        assertTrue(viewModel.state.value is TideUiState.Unavailable)
    }

    private fun sampleTide(): CoastalTide {
        val station = TideStation("9455920", "Anchorage", 61.2383, -149.89)
        val prediction = TidePrediction(
            time = LocalDate.now().atStartOfDay().atZone(ZoneOffset.UTC),
            heightMeters = 1.2,
            high = true,
        )
        return CoastalTide(
            station = station,
            predictions = listOf(prediction),
            fromCache = false,
            heightUnit = TideHeightUnit.METERS,
        )
    }

    private class FakeTideRepository(
        private val result: Result<CoastalTide> = Result.failure(IllegalStateException("unset")),
    ) : TideRepository {
        override suspend fun tidesNear(coordinate: GeoCoordinate, date: LocalDate): Result<CoastalTide> = result
    }
}
