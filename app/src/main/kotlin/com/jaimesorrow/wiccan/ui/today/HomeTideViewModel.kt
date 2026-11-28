package com.jaimesorrow.wiccan.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaimesorrow.wiccan.data.repository.NoNearbyTideStationException
import com.jaimesorrow.wiccan.domain.model.CoastalTide
import com.jaimesorrow.wiccan.domain.repository.TideRepository
import com.jaimesorrow.wiccan.sky.GeoCoordinate
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TideUiState {
    data object NeedsLocation : TideUiState
    data object Loading : TideUiState
    data class Available(val tide: CoastalTide) : TideUiState
    data object NoNearbyStation : TideUiState
    data object Unavailable : TideUiState
}

@HiltViewModel
class HomeTideViewModel @Inject constructor(
    private val repository: TideRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<TideUiState>(TideUiState.NeedsLocation)
    val state: StateFlow<TideUiState> = mutableState.asStateFlow()
    private var loadJob: Job? = null
    private var lastCoordinate: GeoCoordinate? = null

    fun load(coordinate: GeoCoordinate?) {
        if (coordinate == null) {
            lastCoordinate = null
            mutableState.value = TideUiState.NeedsLocation
            return
        }
        if (coordinate == lastCoordinate && mutableState.value is TideUiState.Available) return
        lastCoordinate = coordinate
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.value = TideUiState.Loading
            mutableState.value = repository.tidesNear(coordinate, LocalDate.now())
                .fold(
                    onSuccess = { TideUiState.Available(it) },
                    onFailure = { error ->
                        if (error is NoNearbyTideStationException) {
                            TideUiState.NoNearbyStation
                        } else {
                            TideUiState.Unavailable
                        }
                    },
                )
        }
    }
}
