package com.nyxwell.app.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nyxwell.app.core.error.AppError
import com.nyxwell.app.core.result.AppResult
import com.nyxwell.app.domain.repository.ArchiveRepository
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
            is ArchiveUiEvent.EntryClicked -> loadDetail(event.contentId)
            ArchiveUiEvent.DismissDetail -> _state.update { it.copy(selectedContent = null) }
        }
    }

    private fun loadDetail(contentId: String) {
        _state.update { it.copy(isDetailLoading = true) }
        viewModelScope.launch {
            when (val result = archiveRepository.getContentById(contentId)) {
                is AppResult.Success -> _state.update {
                    it.copy(isDetailLoading = false, selectedContent = result.data)
                }
                is AppResult.Error -> {
                    _state.update { it.copy(isDetailLoading = false) }
                    effectChannel.send(ArchiveUiEffect.ShowSnackbar(AppError.LoadFailed(result.throwable)))
                }
            }
        }
    }
}
