package com.jaimesorrow.wiccan.ads

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * Consent for the ads shown to non-subscribers, gathered with Google's User
 * Messaging Platform (UMP) before the Mobile Ads SDK initializes or any ad is
 * requested. UMP decides where a message is required (for example the EEA, UK
 * and Switzerland, or U.S. states with privacy laws, as configured in AdMob).
 */
interface AdConsentController {
    /** True once UMP reports that ads may be requested (consent obtained or not required). */
    val canRequestAds: StateFlow<Boolean>

    /**
     * True once `MobileAds.initialize` has completed (after the 18+ request
     * configuration was set). No ad may be loaded before this.
     */
    val mobileAdsReady: StateFlow<Boolean>

    /** True when UMP requires a persistent "Privacy choices" entry point in Settings. */
    val privacyOptionsRequired: StateFlow<Boolean>

    /**
     * Refreshes consent information and shows the consent form if one is
     * required. Safe to call repeatedly; only one request runs at a time.
     */
    fun gatherConsent(activity: Activity)

    /** Re-opens the UMP privacy options form so the user can change their choice. */
    fun showPrivacyOptionsForm(activity: Activity)
}
