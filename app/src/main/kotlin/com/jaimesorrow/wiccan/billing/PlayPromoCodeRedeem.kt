package com.jaimesorrow.wiccan.billing

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

private const val PLAY_STORE_PACKAGE = "com.android.vending"
private const val REDEEM_URL_PREFIX = "https://play.google.com/redeem?code="

/** Trims and uppercases a generated one-time-use code for Play's redemption page. */
fun formatPromoCodeForPlay(input: String): String = input.trim().uppercase()

/** Builds the Play redemption URL with a URL-encoded code segment. */
fun playPromoRedeemUri(code: String): Uri =
    Uri.parse(REDEEM_URL_PREFIX + Uri.encode(formatPromoCodeForPlay(code)))

/**
 * Opens Google Play's generated one-time-code redemption flow. Custom
 * subscription codes are not supported by this URL and belong in Play's
 * in-app purchase sheet. Prefers Play Store, then falls back to a browser.
 */
fun launchPlayPromoRedeem(context: Context, rawCode: String): Boolean {
    val uri = playPromoRedeemUri(rawCode)
    val playIntent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage(PLAY_STORE_PACKAGE) }
    if (playIntent.resolveActivity(context.packageManager) != null) {
        try {
            context.startActivity(playIntent)
            return true
        } catch (_: ActivityNotFoundException) {
            // The Play Store may disappear between resolution and launch.
        }
    }
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
