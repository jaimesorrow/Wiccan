package com.wiccan.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiccan.app.domain.model.AuthState
import com.wiccan.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Minimal auth-aware root state for now — just signed-in vs. signed-out.
 * [authState] starts `null` and only takes a real value once
 * [AuthRepository.observeAuthState] emits, so the splash screen can
 * distinguish "still resolving" from a genuine signed-out result. A later
 * pass (once more screens and a proper profile-initialization step exist)
 * replaces this with a fuller root state machine and real navigation graph.
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState?>(null)
    val authState: StateFlow<AuthState?> = _authState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { state ->
                _authState.value = state
            }
        }
    }
}
