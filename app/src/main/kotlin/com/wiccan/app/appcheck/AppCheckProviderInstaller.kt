package com.wiccan.app.appcheck

/**
 * Installs the right App Check provider factory for this build. Implemented
 * separately per build variant (`src/debug`, `src/release`) rather than
 * branching on `BuildConfig.DEBUG` at runtime, so the debug provider class -
 * which only works after being manually allow-listed per install in the
 * Firebase console, and widens what a release APK could be coaxed into
 * doing - never ships on the release classpath at all.
 */
interface AppCheckProviderInstaller {
    fun install()
}
