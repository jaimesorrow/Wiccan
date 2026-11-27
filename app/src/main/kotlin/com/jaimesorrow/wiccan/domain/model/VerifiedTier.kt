package com.jaimesorrow.wiccan.domain.model

/**
 * Entitlement as last confirmed by the server. [UNVERIFIED] covers every state
 * where the server has not answered yet or cannot be trusted: a disk-cache or
 * missing snapshot, a Firestore error, the 10-second verification timeout, and
 * a locally reached expiry that the server has not confirmed yet.
 *
 * Content access fails closed by treating [UNVERIFIED] as [ContentTier.FREE]
 * (see [toContentTier]). Ads fail closed the other way: they appear only for a
 * server-confirmed [FREE], never for [UNVERIFIED].
 */
enum class VerifiedTier {
    UNVERIFIED,
    FREE,
    PREMIUM,
    ;

    /** The content gate: only a server-confirmed subscription unlocks Premium. */
    fun toContentTier(): ContentTier = if (this == PREMIUM) ContentTier.PREMIUM else ContentTier.FREE
}
