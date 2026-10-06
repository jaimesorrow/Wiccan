package com.jaimesorrow.wiccan.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaimesorrow.wiccan.analytics.AnalyticsLogger
import com.jaimesorrow.wiccan.core.auth.CurrentUserProvider
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.domain.repository.ContentRepository
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
    private val analyticsLogger: AnalyticsLogger,
) : ViewModel() {

    private val _state = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    // Buffered so an effect emitted while nothing is collecting isn't dropped.
    private val effectChannel = Channel<TodayUiEffect>(Channel.BUFFERED)
    val effects: Flow<TodayUiEffect> = effectChannel.receiveAsFlow()

    init {
        observeCache()
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
                if (cached != null) {
                    analyticsLogger.logDailyContentViewed(cached.type, cached.tier)
                }
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
                is AppResult.Success -> _state.update { it.copy(isLoading = false, poolExhausted = false) }
                is AppResult.Failure -> {
                    _state.update {
                        it.copy(isLoading = false, poolExhausted = result.error is AppError.NoNewLesson)
                    }
                    if (result.error !is AppError.NoNewLesson) {
                        effectChannel.send(TodayUiEffect.ShowSnackbar(result.error))
                    }
                }
            }
        }
    }

    private fun emit(effect: TodayUiEffect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }
}
