package com.jaimesorrow.wiccan.domain.model

/**
 * Bump whichever constant changed whenever Terms of Service or Privacy
 * Policy content changes in a way that requires existing users to
 * re-accept. Must be kept in sync with the Cloud Functions copy
 * (`functions/src/policyVersions.ts`) - the server rejects an
 * `acceptTerms` call quoting anything other than its own current values.
 */
object PolicyVersions {
    const val TERMS = 3
    const val PRIVACY = 3
}
