package com.jaimesorrow.wiccan.ads

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaimesorrow.wiccan.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Ad state for the one ad placement (the Grimoire banner) and the Settings
 * "Privacy choices" row. Reads the server-verified tier from
 * [SubscriptionRepository.observeTier] - the app's entitlement source of truth.
 */
@HiltViewModel
class AdsViewModel @Inject constructor(
    subscriptionRepository: SubscriptionRepository,
    private val consent: AdConsentController,
) : ViewModel() {

    /** Non-subscriber, confirmed: consent may be gathered for ads. */
    val tierAllowsAds: StateFlow<Boolean> = adsAllowedForTier(subscriptionRepository.observeTier())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    /** An ad may be requested and shown now. */
    val showAds: StateFlow<Boolean> = combine(tierAllowsAds, consent.canRequestAds, ::shouldShowAds)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    val privacyOptionsRequired: StateFlow<Boolean> = consent.privacyOptionsRequired

    fun gatherConsent(activity: Activity) = consent.gatherConsent(activity)

    fun showPrivacyOptions(activity: Activity) = consent.showPrivacyOptionsForm(activity)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
