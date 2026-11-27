package com.jaimesorrow.wiccan.core.config

/**
 * Public legal/support destinations. Native policy documents are always
 * available in the app; these URLs are entered in Play Console (store
 * listing privacy policy, Data Safety account-deletion URL). The pages are
 * served by Firebase Hosting (project wiccan-android, `hosting/` per
 * `firebase.json`) at https://wiccan.app and mirror
 * [com.jaimesorrow.wiccan.ui.legal.WiccanLegalDocuments] exactly. The same
 * files are also staged to GitHub Pages by `.github/workflows/pages.yml` as a
 * secondary preview at https://jaimesorrow.github.io/Wiccan/.
 */
object PolicyUrls {
    const val TERMS_OF_SERVICE = "https://wiccan.app/terms.html"
    const val PRIVACY_POLICY = "https://wiccan.app/privacy.html"
    const val POTION_SAFETY = "https://wiccan.app/potion-safety.html"
    const val ACCOUNT_DELETION_URL = "https://wiccan.app/delete-account.html"
    const val SUPPORT_EMAIL = "hello@wiccan.app"
    const val CUSTOMER_SERVICE_EMAIL = "customerservice@wiccan.app"
}
