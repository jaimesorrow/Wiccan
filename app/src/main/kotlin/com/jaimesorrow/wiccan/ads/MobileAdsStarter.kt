package com.jaimesorrow.wiccan.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Request settings applied to every ad request. Wiccan's audience is 18+ only
 * and it is not in Google Play Families: ads are not child-directed, users are
 * not under the age of consent, and the maximum ad content rating is MA.
 *
 * SDK 25.5.0 marks the two tag setters deprecated in favor of
 * `setAgeRestrictedTreatment`, whose values (UNSPECIFIED, CHILD, TEEN) cannot
 * say "adult"; the explicit FALSE tags still apply and are what an 18+ app sends.
 */
@Suppress("DEPRECATION")
internal data class AdRequestPolicy(
    val tagForChildDirectedTreatment: Int = RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE,
    val tagForUnderAgeOfConsent: Int = RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE,
    val maxAdContentRating: String = RequestConfiguration.MAX_AD_CONTENT_RATING_MA,
)

internal val WICCAN_AD_REQUEST_POLICY = AdRequestPolicy()

/** The two Mobile Ads SDK calls the starter makes, behind a seam for tests. */
internal interface MobileAdsSdk {
    fun setRequestConfiguration(policy: AdRequestPolicy)
    fun initialize(onComplete: () -> Unit)
}

internal class GoogleMobileAdsSdk(private val context: Context) : MobileAdsSdk {
    @Suppress("DEPRECATION") // See AdRequestPolicy.
    override fun setRequestConfiguration(policy: AdRequestPolicy) {
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(policy.tagForChildDirectedTreatment)
                .setTagForUnderAgeOfConsent(policy.tagForUnderAgeOfConsent)
                .setMaxAdContentRating(policy.maxAdContentRating)
                .build(),
        )
    }

    override fun initialize(onComplete: () -> Unit) {
        MobileAds.initialize(context) { onComplete() }
    }
}

/**
 * Starts the Mobile Ads SDK once: applies [WICCAN_AD_REQUEST_POLICY] first,
 * then initializes, and reports [ready] only from the initialization-complete
 * callback. The banner loads only while [ready] is true, so no ad request can
 * run before initialization or without the 18+ configuration.
 * Call [start] off the main thread.
 */
internal class MobileAdsStarter(
    private val sdk: MobileAdsSdk,
    private val onFailure: (RuntimeException) -> Unit = {},
) {
    private val started = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    fun start() {
        if (!started.compareAndSet(false, true)) return
        try {
            sdk.setRequestConfiguration(WICCAN_AD_REQUEST_POLICY)
            sdk.initialize { _ready.value = true }
        } catch (e: RuntimeException) {
            onFailure(e)
            started.set(false)
        }
    }
}
