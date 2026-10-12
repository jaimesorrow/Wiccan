package com.jaimesorrow.wiccan.core.config

/**
 * Centralized public legal/support destinations. Native policy documents are
 * always available in the app; these URLs are still required for the Play
 * listing and external account-deletion request path.
 *
 * TODO(release): these are placeholders. Replace with the real hosted
 * pages/address before any Play submission - Play explicitly rejects
 * listings with placeholder legal URLs, and the Data Safety section
 * requires a publicly accessible account-deletion request page reachable
 * without installing the app (the in-app delete-account flow in Settings
 * satisfies the in-app half of that requirement; ACCOUNT_DELETION_URL is
 * the other half, entered into Play Console's Data Safety form).
 */
object PolicyUrls {
    const val TERMS_OF_SERVICE = "https://wiccan.app/terms"
    const val PRIVACY_POLICY = "https://wiccan.app/privacy"
    const val ACCOUNT_DELETION_URL = "https://wiccan.app/delete-account"
    const val SUPPORT_EMAIL = "support@wiccan.app"
}
