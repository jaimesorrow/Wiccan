#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

failures=0

pass() { printf 'PASS  %s\n' "$1"; }
fail() { printf 'FAIL  %s\n' "$1"; failures=$((failures + 1)); }

require_grep() {
  local pattern="$1" file="$2" label="$3"
  if grep -Eq "$pattern" "$file"; then pass "$label"; else fail "$label"; fi
}

forbid_grep() {
  local pattern="$1" file="$2" label="$3"
  if grep -Eq "$pattern" "$file"; then fail "$label"; else pass "$label"; fi
}

echo "Wiccan release identity / security preflight"
echo "==========================================="

require_grep 'applicationId = "com\.jaimesorrow\.wiccan"' app/build.gradle.kts   'installed applicationId matches Firebase / Play identity'
require_grep 'namespace = "com\.wiccan\.app"' app/build.gradle.kts   'Kotlin namespace remains com.wiccan.app'
require_grep 'compileSdk = 36' app/build.gradle.kts 'compileSdk is API 36'
require_grep 'targetSdk = 36' app/build.gradle.kts 'targetSdk is API 36'
require_grep 'minSdk = 24' app/build.gradle.kts 'minSdk is API 24'

require_grep '"production": "wiccan-android"' .firebaserc   'Firebase production alias is wiccan-android'
forbid_grep 'REPLACE_WITH_PRODUCTION_FIREBASE_PROJECT_ID' .firebaserc   'production Firebase alias is not a placeholder'

require_grep 'ANDROID_PACKAGE_NAME=com\.jaimesorrow\.wiccan' functions/.env.example   'Functions environment example uses installed package ID'
require_grep 'process\.env\.ANDROID_PACKAGE_NAME \?\? "com\.jaimesorrow\.wiccan"' functions/src/index.ts   'Play Developer API backend defaults to installed package ID'

require_grep 'debugImplementation\(libs\.firebase\.appcheck\.debug\)' app/build.gradle.kts   'App Check debug provider is debug-only'
forbid_grep '^\s*implementation\(libs\.firebase\.appcheck\.debug\)' app/build.gradle.kts   'release classpath excludes App Check debug provider'
require_grep 'PlayIntegrityAppCheckProviderFactory'   app/src/release/kotlin/com/wiccan/app/appcheck/ReleaseAppCheckModule.kt   'release builds use Play Integrity App Check'

for fn in getTodayContent verifyPurchase registerDeviceToken unregisterDeviceToken updateTimeZone setNotificationsEnabled acceptTerms deleteUserData; do
  if grep -Eq "export const ${fn} = onCall\\(\\{ enforceAppCheck: true \\}" functions/src/index.ts; then
    pass "${fn} enforces App Check"
  else
    fail "${fn} enforces App Check"
  fi
done

if grep -A4 -F 'match /users/{uid} {' firestore.rules | grep -Fq 'allow write: if false;'; then
  pass 'users collection client writes are denied'
else
  fail 'users collection client writes are denied'
fi

if grep -A3 -F 'match /users/{uid}/devices/{deviceId} {' firestore.rules | grep -Fq 'allow read, write: if false;'; then
  pass 'device registration client access is denied'
else
  fail 'device registration client access is denied'
fi

require_grep 'runTransaction' functions/src/firestoreContentRepository.ts   'daily selection uses a Firestore transaction'
require_grep '\$\{uid\}_\$\{today\}' functions/src/firestoreContentRepository.ts   'daily history has deterministic uid/date identity'

forbid_grep 'export const (generateDraft|approveDraft)\s*=\s*on(Call|Request)' functions/src/index.ts   'admin content generation is not publicly callable'

require_grep 'onMessagePublished\(\{ topic: PLAY_RTDN_TOPIC, retry: true \}' functions/src/index.ts   'Play RTDN retries transient failures'
require_grep '^\s*throw err;' functions/src/index.ts   'Play RTDN rethrows failures for retry'

require_grep 'tools:node="remove"' app/src/main/AndroidManifest.xml   'default WorkManager initializer is removed'
require_grep 'WorkManager\.initialize\(this, workManagerConfiguration\)'   app/src/main/kotlin/com/wiccan/app/WiccanApplication.kt   'WorkManager uses the Hilt-aware configuration'

if git ls-files -z | xargs -0 grep -IlE -e '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----|sk-proj-[A-Za-z0-9_-]{20,}|sk-ant-[A-Za-z0-9_-]{20,}' | grep -q .; then
  fail 'no private key material or obvious server API secret is committed'
else
  pass 'no private key material or obvious server API secret is committed'
fi

printf '\nResult: %d failure(s)\n' "$failures"
if (( failures > 0 )); then
  exit 1
fi
