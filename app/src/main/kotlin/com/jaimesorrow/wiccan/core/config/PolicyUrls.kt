package com.jaimesorrow.wiccan.core.config

/**
 * Public legal/support destinations. Native policy documents are always
 * available in the app; these URLs are entered in Play Console (store
 * listing privacy policy, Data Safety account-deletion URL). The pages are
 * served by Firebase Hosting from `hosting/` in the production project and
 * mirror [com.jaimesorrow.wiccan.ui.legal.WiccanLegalDocuments] exactly;
 * deploy with `firebase deploy --only hosting --project production`.
 */
object PolicyUrls {
    const val TERMS_OF_SERVICE = "https://wiccan-android.web.app/terms"
    const val PRIVACY_POLICY = "https://wiccan-android.web.app/privacy"
    const val ACCOUNT_DELETION_URL = "https://wiccan-android.web.app/delete-account"
    const val SUPPORT_EMAIL = "com.wiccan.app@gmail.com"
}
