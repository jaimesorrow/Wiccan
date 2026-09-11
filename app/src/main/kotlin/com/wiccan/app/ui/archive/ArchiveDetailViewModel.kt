package com.wiccan.app.ui.archive

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.domain.repository.ArchiveRepository
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

const val ARCHIVE_DETAIL_CONTENT_ID_ARG = "contentId"

@HiltViewModel
class ArchiveDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val archiveRepository: ArchiveRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ArchiveDetailUiState())
    val state: StateFlow<ArchiveDetailUiState> = _state.asStateFlow()

    private val effectChannel = Channel<ArchiveDetailUiEffect>(Channel.BUFFERED)
    val effects: Flow<ArchiveDetailUiEffect> = effectChannel.receiveAsFlow()

    init {
        val contentId = checkNotNull(savedStateHandle.get<String>(ARCHIVE_DETAIL_CONTENT_ID_ARG)) {
            "ArchiveDetailViewModel requires a '$ARCHIVE_DETAIL_CONTENT_ID_ARG' nav argument"
        }
        viewModelScope.launch {
            when (val result = archiveRepository.getContentById(contentId)) {
                is AppResult.Success -> _state.update { it.copy(isLoading = false, content = result.data) }
                is AppResult.Failure -> {
                    _state.update { it.copy(isLoading = false) }
                    effectChannel.send(ArchiveDetailUiEffect.ShowSnackbar(result.error))
                }
            }
        }
    }
}
