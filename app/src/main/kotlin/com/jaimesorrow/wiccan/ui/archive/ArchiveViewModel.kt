package com.jaimesorrow.wiccan.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaimesorrow.wiccan.domain.repository.ArchiveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the Archive list ("Grimoire" Timeline) only. Tapping an
 * entry is a navigation event - the detail screen is its own route
 * (`archive_detail/{contentId}`) with its own ViewModel, not internal
 * state here.
 */
@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val archiveRepository: ArchiveRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ArchiveUiState())
    val state: StateFlow<ArchiveUiState> = _state.asStateFlow()

    private val effectChannel = Channel<ArchiveUiEffect>(Channel.BUFFERED)
    val effects: Flow<ArchiveUiEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            archiveRepository.observeHistory().collect { entries ->
                _state.update { it.copy(isLoading = false, entries = entries) }
            }
        }
    }

    fun onEvent(event: ArchiveUiEvent) {
        when (event) {
            is ArchiveUiEvent.EntryClicked -> viewModelScope.launch {
                effectChannel.send(ArchiveUiEffect.NavigateToDetail(event.contentId))
            }
            is ArchiveUiEvent.TabSelected -> _state.update { it.copy(selectedTab = event.tab) }
            is ArchiveUiEvent.CategoryToggled -> _state.update {
                val collapsed = it.collapsedCategories
                it.copy(
                    collapsedCategories = if (event.category in collapsed) {
                        collapsed - event.category
                    } else {
                        collapsed + event.category
                    },
                )
            }
        }
    }
}
