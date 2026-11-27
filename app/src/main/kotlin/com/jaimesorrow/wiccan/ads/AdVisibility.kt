package com.jaimesorrow.wiccan.ads

import com.jaimesorrow.wiccan.domain.model.VerifiedTier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest

/**
 * How long a server-confirmed FREE tier must hold before ads may appear, so a
 * purchase or restore that is still being verified has time to land first.
 */
internal const val FREE_TIER_SETTLE_MILLIS = 3_000L

/**
 * The ad-visibility gate. It fails closed: ads are for confirmed non-subscribers only.
 * - [VerifiedTier.FREE] (a *server* snapshot says no active subscription) shows
 *   ads once it has held for [settleMillis];
 * - [VerifiedTier.PREMIUM] (an active monthly or yearly subscription, a trial,
 *   or grace) hides ads immediately, including mid-session;
 * - [VerifiedTier.UNVERIFIED] (disk cache, Firestore error, the 10s verification
 *   timeout, offline, signed out) never shows ads.
 * Starts hidden, so nothing shows before the server has answered.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun adsAllowedForTier(
    tiers: Flow<VerifiedTier>,
    settleMillis: Long = FREE_TIER_SETTLE_MILLIS,
): Flow<Boolean> = tiers
    .distinctUntilChanged()
    .transformLatest { tier ->
        if (tier == VerifiedTier.FREE) {
            delay(settleMillis)
            emit(true)
        } else {
            emit(false)
        }
    }
    .onStart { emit(false) }
    .distinctUntilChanged()

/**
 * Whether an ad may be requested right now: the tier gate passed, the User
 * Messaging Platform says consent (where required) allows ad requests, and
 * `MobileAds.initialize` has completed (with the 18+ request configuration
 * applied before it), so no `loadAd` can run ahead of initialization.
 */
internal fun shouldShowAds(tierAllowsAds: Boolean, canRequestAds: Boolean, mobileAdsReady: Boolean): Boolean =
    tierAllowsAds && canRequestAds && mobileAdsReady
