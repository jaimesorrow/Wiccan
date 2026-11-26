package com.jaimesorrow.wiccan.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaimesorrow.wiccan.analytics.AnalyticsLogger
import com.jaimesorrow.wiccan.core.auth.CurrentUserProvider
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.repository.ContentRepository
import com.jaimesorrow.wiccan.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val analyticsLogger: AnalyticsLogger,
    subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TodayUiState(lessonLocked = true))
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    // Buffered so an effect emitted while nothing is collecting isn't dropped.
    private val effectChannel = Channel<TodayUiEffect>(Channel.BUFFERED)
    val effects: Flow<TodayUiEffect> = effectChannel.receiveAsFlow()
    private var hasHandledInitialResume = false

    init {
        observeCache()
        observeAccess(subscriptionRepository)
        refresh()
    }

    fun onEvent(event: TodayUiEvent) {
        when (event) {
            TodayUiEvent.Resumed -> {
                if (hasHandledInitialResume) refresh() else hasHandledInitialResume = true
            }
            TodayUiEvent.Retry -> refresh()
        }
    }

    private fun observeCache() {
        viewModelScope.launch {
            contentRepository.observeTodayContent()
                // A cache read failure must never take the screen down; show the retry panel instead.
                .catch { t ->
                    if (t is CancellationException) throw t
                    _state.update { it.copy(isLoading = false, loadFailed = true) }
                }
                .collect { cached ->
                    _state.update { it.copy(content = cached) }
                    if (cached != null && !_state.value.lessonLocked) {
                        analyticsLogger.logDailyContentViewed(cached.type, cached.tier)
                    }
                }
        }
    }

    private fun observeAccess(subscriptionRepository: SubscriptionRepository) {
        viewModelScope.launch {
            subscriptionRepository.observeTier().collect { tier ->
                _state.update { it.copy(lessonLocked = tier != ContentTier.PREMIUM) }
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
                is AppResult.Success -> _state.update {
                    it.copy(isLoading = false, poolExhausted = false, loadFailed = false)
                }
                is AppResult.Failure -> {
                    val exhausted = result.error is AppError.NoNewLesson
                    val locked = result.error is AppError.SubscriptionRequired
                    _state.update {
                        it.copy(
                            isLoading = false,
                            poolExhausted = exhausted,
                            loadFailed = !exhausted && !locked,
                            lessonLocked = locked || it.lessonLocked,
                        )
                    }
                    if (result.error !is AppError.NoNewLesson && !locked) {
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
