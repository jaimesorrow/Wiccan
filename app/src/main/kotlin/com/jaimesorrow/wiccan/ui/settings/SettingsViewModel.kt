package com.jaimesorrow.wiccan.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaimesorrow.wiccan.analytics.AnalyticsConsentManager
import com.jaimesorrow.wiccan.billing.isYearlySwitchEligible
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.data.local.ThemePreferences
import com.jaimesorrow.wiccan.domain.model.AuthState
import com.jaimesorrow.wiccan.domain.model.SubscriptionState
import com.jaimesorrow.wiccan.domain.model.WiccanThemeMode
import com.jaimesorrow.wiccan.domain.repository.AuthRepository
import com.jaimesorrow.wiccan.domain.repository.NotificationRepository
import com.jaimesorrow.wiccan.domain.repository.SubscriptionRepository
import com.jaimesorrow.wiccan.domain.usecase.DeleteAccountUseCase
import com.jaimesorrow.wiccan.widget.WiccanWidgetUpdater
import com.wiccan.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val subscriptionRepository: SubscriptionRepository,
    private val notificationRepository: NotificationRepository,
    private val themePreferences: ThemePreferences,
    private val wiccanWidgetUpdater: WiccanWidgetUpdater,
    private val analyticsConsentManager: AnalyticsConsentManager,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState(accountEmail = authRepository.currentEmail().orEmpty()))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val effectChannel = Channel<SettingsUiEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsUiEffect> = effectChannel.receiveAsFlow()

    /** Set once the user edits either name field; after that the saved name never overwrites the form. */
    private var nameEdited = false

    /** True after Play's yearly switch sheet opens, until it is dismissed or the plan is no longer monthly. */
    private var switchInFlight = false
    private var latestSubscriptionState = SubscriptionState.UNKNOWN
    private var latestProductId: String? = null

    init {
        viewModelScope.launch {
            // This ViewModel can outlive a sign-out (the onboarding copy is
            // activity-scoped), so per-account fields are cleared whenever the
            // signed-in account changes and the saved name is read again.
            var previousUid: String? = null
            var first = true
            authRepository.observeAuthState()
                .map { (it as? AuthState.SignedIn)?.uid }
                .distinctUntilChanged()
                .collectLatest { uid ->
                    if (!first && uid != previousUid) {
                        nameEdited = false
                        _state.update {
                            it.copy(
                                firstName = "",
                                lastName = "",
                                newEmail = "",
                                accountEmail = authRepository.currentEmail().orEmpty(),
                            )
                        }
                    }
                    first = false
                    previousUid = uid
                    if (uid != null) prefillName()
                }
        }
        viewModelScope.launch {
            combine(
                subscriptionRepository.observeTier(),
                subscriptionRepository.observeSubscriptionState(),
                subscriptionRepository.observeProductId(),
            ) { tier, subscriptionState, productId ->
                Triple(tier, subscriptionState, productId)
            }.collectLatest { (tier, subscriptionState, productId) ->
                latestSubscriptionState = subscriptionState
                latestProductId = productId
                val eligible = isYearlySwitchEligible(tier, subscriptionState, productId)
                if (!eligible) switchInFlight = false
                _state.update {
                    it.copy(
                        tier = tier,
                        yearlySwitchOffer = if (eligible && !switchInFlight) it.yearlySwitchOffer else null,
                    )
                }
                if (!eligible || switchInFlight) return@collectLatest
                loadYearlySwitchOffer()
            }
        }
        viewModelScope.launch {
            subscriptionRepository.observePurchaseFlowDismissed().collect {
                switchInFlight = false
                if (isYearlySwitchEligible(state.value.tier, latestSubscriptionState, latestProductId)) {
                    loadYearlySwitchOffer()
                }
            }
        }
        viewModelScope.launch {
            notificationRepository.notificationsEnabled.collect { enabled ->
                _state.update { it.copy(notificationsEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            analyticsConsentManager.consent.collect { share ->
                _state.update { it.copy(shareAnalytics = share) }
            }
        }
        viewModelScope.launch {
            themePreferences.themeMode.collect { mode ->
                _state.update { it.copy(themeMode = mode) }
            }
        }
    }

    private suspend fun prefillName() {
        val saved = authRepository.currentProfileName() ?: return
        if (nameEdited) return
        _state.update { it.copy(firstName = saved.firstName, lastName = saved.lastName) }
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.SignOutClicked -> signOut()
            is SettingsUiEvent.FirstNameChanged -> {
                nameEdited = true
                _state.update { it.copy(firstName = event.value) }
            }
            is SettingsUiEvent.LastNameChanged -> {
                nameEdited = true
                _state.update { it.copy(lastName = event.value) }
            }
            is SettingsUiEvent.NewEmailChanged -> _state.update { it.copy(newEmail = event.value) }
            SettingsUiEvent.SaveNameClicked -> saveName()
            SettingsUiEvent.ChangeEmailClicked -> updateEmail()
            SettingsUiEvent.ResetPasswordClicked -> resetPassword()
            SettingsUiEvent.DeleteAccountClicked -> _state.update { it.copy(showDeleteConfirmation = true) }
            SettingsUiEvent.CancelDeleteAccount -> _state.update { it.copy(showDeleteConfirmation = false) }
            SettingsUiEvent.ConfirmDeleteAccount -> deleteAccount()
            is SettingsUiEvent.NotificationsToggled -> toggleNotifications(event.enabled)
            is SettingsUiEvent.ShareAnalyticsToggled -> toggleShareAnalytics(event.share)
            is SettingsUiEvent.ThemeModeSelected -> selectThemeMode(event.mode)
            SettingsUiEvent.LanguageChanged -> refreshRegisteredLocale()
        }
    }

    private fun selectThemeMode(mode: WiccanThemeMode) {
        // Purely local persistence (no server/AppResult involved) - the
        // DataStore flow collected in init() reflects the new value back
        // into state as soon as the write completes.
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
            wiccanWidgetUpdater.updateAll()
        }
    }

    private fun toggleShareAnalytics(share: Boolean) {
        _state.update { it.copy(shareAnalytics = share) }
        viewModelScope.launch {
            if (!analyticsConsentManager.setConsent(share)) {
                // Withdrawal still took effect for this session; the switch shows what is saved.
                _state.update { it.copy(shareAnalytics = !share) }
                effectChannel.send(
                    SettingsUiEffect.ShowMessage(R.string.settings_notice_analytics),
                )
            }
        }
    }

    private fun toggleNotifications(enabled: Boolean) {
        // Optimistic: the switch should feel instant. Reverted if the
        // server call fails, so the UI never claims a state that didn't
        // actually take effect.
        val previous = state.value
        if (previous.isNotificationBusy) return
        _state.update { it.copy(notificationsEnabled = enabled, isNotificationBusy = true) }
        viewModelScope.launch {
            when (val result = notificationRepository.setNotificationsEnabled(enabled)) {
                is AppResult.Success -> _state.update { it.copy(isNotificationBusy = false) }
                is AppResult.Failure -> {
                    _state.update { it.copy(notificationsEnabled = previous.notificationsEnabled, isNotificationBusy = false) }
                    effectChannel.send(SettingsUiEffect.ShowSnackbar(result.error))
                }
            }
        }
    }

    private fun saveName() {
        val first = state.value.firstName.trim()
        val last = state.value.lastName.trim()
        if (first.isBlank() || last.isBlank() || first.length > 60 || last.length > 60) {
            viewModelScope.launch {
                effectChannel.send(SettingsUiEffect.ShowMessage(R.string.settings_notice_name_invalid))
            }
            return
        }
        if (state.value.isAccountBusy) return
        _state.update { it.copy(isAccountBusy = true) }
        viewModelScope.launch {
            val result = authRepository.completeProfile(first, last)
            _state.update { it.copy(isAccountBusy = false) }
            when (result) {
                is AppResult.Success -> effectChannel.send(SettingsUiEffect.ShowMessage(R.string.settings_notice_name_saved))
                is AppResult.Failure -> effectChannel.send(SettingsUiEffect.ShowSnackbar(result.error))
            }
        }
    }

    private fun updateEmail() {
        val email = state.value.newEmail.trim()
        if (email.length > 254 || !email.contains("@") || email.contains(" ")) {
            viewModelScope.launch { effectChannel.send(SettingsUiEffect.ShowMessage(R.string.settings_notice_email_invalid)) }
            return
        }
        if (state.value.isAccountBusy) return
        _state.update { it.copy(isAccountBusy = true) }
        viewModelScope.launch {
            val result = authRepository.requestEmailUpdate(email)
            _state.update { it.copy(isAccountBusy = false) }
            when (result) {
                is AppResult.Success -> effectChannel.send(
                    SettingsUiEffect.ShowMessage(R.string.settings_notice_email_sent),
                )
                is AppResult.Failure -> effectChannel.send(SettingsUiEffect.ShowSnackbar(result.error))
            }
        }
    }

    private fun resetPassword() {
        val email = state.value.accountEmail
        if (email.isBlank() || state.value.isAccountBusy) return
        _state.update { it.copy(isAccountBusy = true) }
        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(email)
            _state.update { it.copy(isAccountBusy = false) }
            when (result) {
                is AppResult.Success -> effectChannel.send(
                    SettingsUiEffect.ShowMessage(R.string.settings_notice_reset_sent),
                )
                is AppResult.Failure -> effectChannel.send(SettingsUiEffect.ShowSnackbar(result.error))
            }
        }
    }

    private fun refreshRegisteredLocale() {
        viewModelScope.launch {
            notificationRepository.refreshAndRegisterToken()
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            // Best-effort: a signed-out device shouldn't keep receiving pushes
            // meant for whoever signs in next. A failure here (offline, etc.)
            // must never block the actual sign-out.
            notificationRepository.unregisterToken()
            authRepository.signOut()
        }
    }

    private fun deleteAccount() {
        var alreadyDeleting = false
        _state.update { current ->
            if (current.isDeleting) {
                alreadyDeleting = true
                current
            } else {
                current.copy(isDeleting = true, showDeleteConfirmation = false)
            }
        }
        if (alreadyDeleting) return
        viewModelScope.launch {
            when (val result = deleteAccountUseCase()) {
                is AppResult.Success -> _state.update { it.copy(isDeleting = false) }
                is AppResult.Failure -> {
                    _state.update { it.copy(isDeleting = false) }
                    effectChannel.send(SettingsUiEffect.ShowSnackbar(result.error))
                }
            }
        }
    }

    fun switchToYearly(activity: Activity) {
        if (state.value.isSwitchBusy || state.value.isDeleting || state.value.yearlySwitchOffer == null) return
        _state.update { it.copy(isSwitchBusy = true) }
        viewModelScope.launch {
            when (val result = subscriptionRepository.launchSwitchToYearly(activity)) {
                is AppResult.Success -> {
                    switchInFlight = true
                    _state.update { it.copy(isSwitchBusy = false, yearlySwitchOffer = null) }
                }
                is AppResult.Failure -> {
                    switchInFlight = false
                    _state.update { it.copy(isSwitchBusy = false) }
                    effectChannel.send(SettingsUiEffect.ShowSnackbar(result.error))
                }
            }
        }
    }

    private suspend fun loadYearlySwitchOffer() {
        when (val result = subscriptionRepository.getYearlySwitchOffer()) {
            is AppResult.Success -> _state.update { current ->
                val stillEligible = isYearlySwitchEligible(
                    current.tier,
                    latestSubscriptionState,
                    latestProductId,
                ) && !switchInFlight && result.data != null
                current.copy(yearlySwitchOffer = if (stillEligible) result.data else null)
            }
            is AppResult.Failure -> _state.update { it.copy(yearlySwitchOffer = null) }
        }
    }
}
