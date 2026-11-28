package com.jaimesorrow.wiccan.billing

import android.content.Context
import android.content.Intent
import android.net.Uri

private const val PLAY_STORE_PACKAGE = "com.android.vending"
private const val REDEEM_URL_PREFIX = "https://play.google.com/redeem?code="

/** Trims and uppercases the code for Play's redemption page; no client-side validation. */
fun formatPromoCodeForPlay(input: String): String = input.trim().uppercase()

/** Builds the Play redemption URL with a URL-encoded code segment. */
fun playPromoRedeemUri(code: String): Uri =
    Uri.parse(REDEEM_URL_PREFIX + Uri.encode(formatPromoCodeForPlay(code)))

/**
 * Opens Google Play's promo redemption flow. Prefers the Play Store app when
 * installed; otherwise falls back to the system browser.
 */
fun launchPlayPromoRedeem(context: Context, rawCode: String) {
    val uri = playPromoRedeemUri(rawCode)
    val playIntent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage(PLAY_STORE_PACKAGE) }
    val intent = if (playIntent.resolveActivity(context.packageManager) != null) {
        playIntent
    } else {
        Intent(Intent.ACTION_VIEW, uri)
    }
    context.startActivity(intent)
}
