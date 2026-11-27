package com.jaimesorrow.wiccan.ads

import com.jaimesorrow.wiccan.domain.model.ContentTier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest

/**
 * How long a non-premium tier must hold before ads may appear. The tier stream
 * reports FREE from the disk cache (and during its bounded wait) before the
 * server snapshot confirms an active subscription, so a subscriber on a cold
 * start would otherwise see an ad flash before Premium arrives.
 */
internal const val FREE_TIER_SETTLE_MILLIS = 3_000L

/**
 * The ad-visibility gate. Ads are for non-subscribers only:
 * - PREMIUM (an active monthly or yearly subscription, or a trial, as verified
 *   by the server - see `UserTierDataSource`) hides ads immediately;
 * - FREE shows ads only once it has held for [settleMillis].
 * Starts hidden, so nothing shows before the tier is known.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun adsAllowedForTier(
    tiers: Flow<ContentTier>,
    settleMillis: Long = FREE_TIER_SETTLE_MILLIS,
): Flow<Boolean> = tiers
    .distinctUntilChanged()
    .transformLatest { tier ->
        if (tier == ContentTier.PREMIUM) {
            emit(false)
        } else {
            delay(settleMillis)
            emit(true)
        }
    }
    .onStart { emit(false) }
    .distinctUntilChanged()

/**
 * Whether an ad may be requested right now: the tier gate passed and the
 * User Messaging Platform says consent (where required) allows ad requests.
 */
internal fun shouldShowAds(tierAllowsAds: Boolean, canRequestAds: Boolean): Boolean = tierAllowsAds && canRequestAds
