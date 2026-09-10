package com.nyxwell.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nyxwell.app.core.auth.CurrentUserProvider
import com.nyxwell.app.core.error.AppError
import com.nyxwell.app.core.result.AppResult
import com.nyxwell.app.domain.repository.ContentRepository
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
class TodayViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val currentUserProvider: CurrentUserProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    // Buffered so an effect emitted while nothing is collecting isn't dropped.
    private val effectChannel = Channel<TodayUiEffect>(Channel.BUFFERED)
    val effects: Flow<TodayUiEffect> = effectChannel.receiveAsFlow()

    init {
        observeCache()
        refresh()
    }

    fun onEvent(event: TodayUiEvent) {
        when (event) {
            TodayUiEvent.Retry -> refresh()
        }
    }

    private fun observeCache() {
        viewModelScope.launch {
            contentRepository.observeTodayContent().collect { cached ->
                _state.update { it.copy(content = cached) }
            }
        }
    }

    private fun refresh() {
        val uid = currentUserProvider.currentUserId
        if (uid == null) {
            _state.update { it.copy(isLoading = false) }
            emit(TodayUiEffect.ShowSnackbar(AppError.Unauthorized))
            return
        }
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = contentRepository.refreshTodayContent(uid)) {
                is AppResult.Success -> _state.update { it.copy(isLoading = false) }
                is AppResult.Failure -> {
                    _state.update { it.copy(isLoading = false) }
                    effectChannel.send(TodayUiEffect.ShowSnackbar(result.error))
                }
            }
        }
    }

    private fun emit(effect: TodayUiEffect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }
}
