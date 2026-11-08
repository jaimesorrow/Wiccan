// Security floors for libraries that AGP 9.5.0 (latest stable) pulls onto the
// build classpath at vulnerable versions. They run only on build machines, but
// stay pinned until an AGP release ships patched versions (docs/MAINTENANCE.md).
buildscript {
    dependencies {
        constraints {
            classpath(libs.build.guava) { because("GHSA-xxph-c9ww-hj94") }
            classpath(libs.build.bcprov) { because("GHSA-9pwp-9qqc-pr26, GHSA-qp49-qgx5-5m26") }
            classpath(libs.build.bcpkix) { because("GHSA-wg6q-6289-32hp") }
            classpath(libs.build.bcutil) { because("keep Bouncy Castle modules on one version") }
            classpath(libs.build.commons.lang3) { because("GHSA-j288-q9x7-2f5v") }
            classpath(libs.build.jose4j) { because("GHSA-3677-xxcr-wjqv") }
            classpath(libs.build.jdom2) { because("GHSA-2363-cqg2-863c") }
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.ktlint) apply false
}
