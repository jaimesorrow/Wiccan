package com.jaimesorrow.wiccan.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UMP-backed [AdConsentController]. The Mobile Ads SDK is initialized once,
 * off the main thread, and only after UMP allows ad requests. Every SDK call
 * is guarded: a consent or network failure leaves ads off instead of crashing.
 */
@Singleton
class UmpAdConsentController @Inject constructor(
    @ApplicationContext private val context: Context,
) : AdConsentController {

    private val consentInformation: ConsentInformation by lazy {
        UserMessagingPlatform.getConsentInformation(context)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requestInFlight = AtomicBoolean(false)
    private val mobileAdsStarted = AtomicBoolean(false)

    private val _canRequestAds = MutableStateFlow(false)
    override val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    override val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    override fun gatherConsent(activity: Activity) {
        // A choice made in an earlier session lets ads start without waiting.
        refreshState()
        if (!requestInFlight.compareAndSet(false, true)) return
        try {
            val params = ConsentRequestParameters.Builder().build()
            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        if (formError != null) Log.w(TAG, "Consent form: ${formError.message}")
                        requestInFlight.set(false)
                        refreshState()
                    }
                },
                { requestError ->
                    Log.w(TAG, "Consent info update: ${requestError.message}")
                    requestInFlight.set(false)
                    refreshState()
                },
            )
        } catch (e: RuntimeException) {
            Log.w(TAG, "Consent request failed", e)
            requestInFlight.set(false)
        }
    }

    override fun showPrivacyOptionsForm(activity: Activity) {
        try {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
                if (formError != null) Log.w(TAG, "Privacy options form: ${formError.message}")
                refreshState()
            }
        } catch (e: RuntimeException) {
            Log.w(TAG, "Privacy options form failed", e)
        }
    }

    private fun refreshState() {
        val canRequest = try {
            consentInformation.canRequestAds()
        } catch (e: RuntimeException) {
            Log.w(TAG, "Consent state unavailable", e)
            false
        }
        _privacyOptionsRequired.value = try {
            consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        } catch (_: RuntimeException) {
            false
        }
        if (canRequest) startMobileAds()
        _canRequestAds.value = canRequest
    }

    private fun startMobileAds() {
        if (!mobileAdsStarted.compareAndSet(false, true)) return
        scope.launch {
            try {
                MobileAds.initialize(context) {}
            } catch (e: RuntimeException) {
                Log.w(TAG, "Mobile Ads initialization failed", e)
                mobileAdsStarted.set(false)
            }
        }
    }

    private companion object {
        const val TAG = "WiccanAds"
    }
}
