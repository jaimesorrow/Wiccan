package com.jaimesorrow.wiccan.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaimesorrow.wiccan.domain.model.AccountStatus
import com.jaimesorrow.wiccan.domain.model.AuthState
import com.jaimesorrow.wiccan.domain.model.ContentTier
import com.jaimesorrow.wiccan.domain.model.RootState
import com.jaimesorrow.wiccan.domain.repository.AuthRepository
import com.jaimesorrow.wiccan.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * Resolves [RootState] before the signed-in app is entered: Firebase Auth
 * alone is not enough, since the server-side `users/{uid}` profile is
 * created asynchronously after sign-up, and a signed-in user may still owe
 * acceptance of a current Terms/Privacy version. [rootState] starts at
 * [RootState.Loading] so the splash screen can distinguish "still
 * resolving" from a genuine signed-out result.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RootViewModel @Inject constructor(
    authRepository: AuthRepository,
    subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    private val _rootState = MutableStateFlow<RootState>(RootState.Loading)
    val rootState: StateFlow<RootState> = _rootState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeAuthState()
                .distinctUntilChanged()
                .flatMapLatest { authState ->
                    when (authState) {
                        AuthState.SignedOut -> flowOf(RootState.SignedOut)
                        is AuthState.SignedIn -> authRepository.observeAccountStatus(authState.uid)
                            .distinctUntilChanged()
                            .flatMapLatest { status ->
                                when (status) {
                                    AccountStatus.Initializing -> flowOf(RootState.InitializingProfile)
                                    AccountStatus.RequiresPolicyAcceptance -> flowOf(RootState.RequiresPolicyAcceptance)
                                    AccountStatus.Ready -> subscriptionRepository.observeTier()
                                        .map<ContentTier, RootState> { tier ->
                                            if (tier == ContentTier.PREMIUM) {
                                                RootState.SignedIn(authState.uid)
                                            } else {
                                                RootState.RequiresSubscription(authState.uid)
                                            }
                                        }
                                        .onStart { emit(RootState.RequiresSubscription(authState.uid)) }
                                        .catch { emit(RootState.RequiresSubscription(authState.uid)) }
                                }
                            }
                            .onStart { emit(RootState.InitializingProfile) }
                    }
                }
                .collect { _rootState.value = it }
        }
    }
}
