package com.nyxwell.app.ui.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nyxwell.app.domain.model.ContentTier
import com.nyxwell.app.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Minimal functional trigger for the single premium subscription — no
 * paywall screen design in this pass. A future screen decides where
 * [UpgradeButton] is placed and how [tier] is displayed.
 */
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    val tier: Flow<ContentTier> = subscriptionRepository.observeTier()

    fun upgrade(activity: Activity) {
        viewModelScope.launch {
            subscriptionRepository.launchPurchaseFlow(activity)
        }
    }
}
