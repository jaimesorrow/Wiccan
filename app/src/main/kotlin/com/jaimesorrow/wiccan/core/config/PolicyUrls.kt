package com.jaimesorrow.wiccan.core.config

/**
 * Public legal/support destinations. Native policy documents are always
 * available in the app; these URLs are entered in Play Console (store
 * listing privacy policy, Data Safety account-deletion URL). The pages are
 * served by GitHub Pages from `hosting/` at the app subdomain and mirror
 * [com.jaimesorrow.wiccan.ui.legal.WiccanLegalDocuments] exactly; deployment
 * runs through `.github/workflows/pages.yml`.
 */
object PolicyUrls {
    const val TERMS_OF_SERVICE = "https://app.wiccanandroid.com/terms.html"
    const val PRIVACY_POLICY = "https://app.wiccanandroid.com/privacy.html"
    const val POTION_SAFETY = "https://app.wiccanandroid.com/potion-safety.html"
    const val ACCOUNT_DELETION_URL = "https://app.wiccanandroid.com/delete-account.html"
    const val SUPPORT_EMAIL = "wiccan.android.app@gmail.com"
}
