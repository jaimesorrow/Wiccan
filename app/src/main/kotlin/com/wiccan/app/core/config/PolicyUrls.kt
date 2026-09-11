package com.wiccan.app.core.config

/**
 * Centralized production legal URLs - referenced from Settings and the
 * policy-acceptance screen, never hardcoded inline in a Composable.
 *
 * TODO(release): these are placeholders. Replace with the real hosted
 * pages before any Play submission - Play explicitly rejects listings
 * with placeholder legal URLs.
 */
object PolicyUrls {
    const val TERMS_OF_SERVICE = "https://wiccan.app/terms"
    const val PRIVACY_POLICY = "https://wiccan.app/privacy"
}
