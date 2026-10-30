/**
 * Bump whichever constant changed whenever Terms of Service or Privacy
 * Policy content changes in a way that requires existing users to
 * re-accept. Must be kept in sync with the Android app's
 * `PolicyVersions` object - the client uses its own copy to decide
 * whether a returning user needs to be re-prompted, and the server
 * rejects `acceptTerms` calls quoting anything other than these current
 * values.
 */
export const CURRENT_TERMS_VERSION = 3;
export const CURRENT_PRIVACY_VERSION = 3;
