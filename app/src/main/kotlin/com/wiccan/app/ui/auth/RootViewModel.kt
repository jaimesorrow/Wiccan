package com.wiccan.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiccan.app.domain.model.AuthState
import com.wiccan.app.domain.model.RootState
import com.wiccan.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Resolves [RootState] before the signed-in app is entered: Firebase Auth
 * alone is not enough, since the server-side `users/{uid}` profile is
 * created asynchronously after sign-up. [rootState] starts at [RootState.Loading]
 * so the splash screen can distinguish "still resolving" from a genuine
 * signed-out result.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RootViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    private val _rootState = MutableStateFlow<RootState>(RootState.Loading)
    val rootState: StateFlow<RootState> = _rootState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeAuthState()
                .flatMapLatest { authState ->
                    when (authState) {
                        AuthState.SignedOut -> flowOf(RootState.SignedOut)
                        is AuthState.SignedIn -> authRepository.observeProfileReady(authState.uid)
                            .map { ready -> if (ready) RootState.SignedIn else RootState.InitializingProfile }
                    }
                }
                .collect { _rootState.value = it }
        }
    }
}
