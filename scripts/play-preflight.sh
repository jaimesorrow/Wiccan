#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

failures=0
warnings=0

pass() { printf 'PASS  %s\n' "$1"; }
warn() { printf 'WARN  %s\n' "$1"; warnings=$((warnings + 1)); }
fail() { printf 'FAIL  %s\n' "$1"; failures=$((failures + 1)); }

require_grep() {
  local pattern="$1" file="$2" label="$3"
  if grep -Eq "$pattern" "$file"; then pass "$label"; else fail "$label"; fi
}

forbid_grep() {
  local pattern="$1" file="$2" label="$3"
  if grep -Eq "$pattern" "$file"; then fail "$label"; else pass "$label"; fi
}

echo "Wiccan security / Play preflight"
echo "================================"

require_grep 'applicationId = "com\.nyxwell\.app"' app/build.gradle.kts \
  'canonical technical applicationId is com.nyxwell.app'
require_grep 'compileSdk = 36' app/build.gradle.kts 'compileSdk is API 36'
require_grep 'targetSdk = 36' app/build.gradle.kts 'targetSdk is API 36'
require_grep 'minSdk = 24' app/build.gradle.kts 'minSdk is API 24'
require_grep 'billing = "9\.1\.0"' gradle/libs.versions.toml 'Play Billing Library baseline is 9.1.0'

require_grep 'debugImplementation\(libs\.firebase\.appcheck\.debug\)' app/build.gradle.kts \
  'App Check debug provider is debug-only'
forbid_grep '^\s*implementation\(libs\.firebase\.appcheck\.debug\)' app/build.gradle.kts \
  'release classpath excludes App Check debug provider'

require_grep 'match /users/\{uid\}' firestore.rules 'users rule exists'
require_grep 'allow write: if false;' firestore.rules 'protected Firestore writes are denied to clients'

forbid_grep 'export const (generateDraft|approveDraft)\s*=\s*on(Call|Request)' functions/src/index.ts \
  'admin generation/approval are absent from public HTTPS/callable exports'

for fn in getTodayContent verifyPurchase registerDeviceToken unregisterDeviceToken updateTimeZone acceptTerms deleteUserData; do
  if grep -Eq "export const ${fn} = onCall\\(\\{ enforceAppCheck: true \\}" functions/src/index.ts; then
    pass "${fn} enforces App Check"
  else
    fail "${fn} enforces App Check"
  fi
done

require_grep 'runTransaction' functions/src/firestoreContentRepository.ts \
  'daily assignment repository uses Firestore transaction'
require_grep '\$\{uid\}_\$\{today\}' functions/src/firestoreContentRepository.ts \
  'daily history uses deterministic uid_date document identity'

require_grep 'subscriptions' functions/src/firestoreSubscriptionRecordRepository.ts \
  'durable subscriptions collection exists'
require_grep 'onMessagePublished' functions/src/index.ts 'RTDN Pub/Sub handler is exported'
require_grep 'com\.nyxwell\.app' functions/src/index.ts \
  'backend Play verification uses canonical technical package identity'

require_grep 'android:dataExtractionRules="@xml/data_extraction_rules"' app/src/main/AndroidManifest.xml \
  'Android 12+ data extraction rules are explicit'
require_grep 'android:fullBackupContent="@xml/backup_rules"' app/src/main/AndroidManifest.xml \
  'Android 6-11 backup rules are explicit'

for service in contentService subscriptionService userService analyticsService; do
  if [[ -f "functions/src/services/${service}.ts" ]]; then
    pass "internal ${service} exists"
  else
    fail "internal ${service} exists"
  fi
done

if [[ -f app/build/outputs/bundle/release/app-release.aab ]]; then
  pass 'release AAB exists'
else
  warn 'release AAB not present; run ./gradlew :app:bundleRelease before final release preflight'
fi

if grep -Rq 'REPLACE_WITH_\|https://wiccan\.app/\|support@wiccan\.app' .firebaserc app/src/main/kotlin docs 2>/dev/null; then
  if [[ "${STRICT_EXTERNAL:-0}" == "1" ]]; then
    fail 'external Firebase/legal/support placeholders remain'
  else
    warn 'external Firebase/legal/support placeholders remain (set STRICT_EXTERNAL=1 for release gate)'
  fi
else
  pass 'no known external release placeholders detected'
fi

printf '\nResult: %d failure(s), %d warning(s)\n' "$failures" "$warnings"
if (( failures > 0 )); then
  exit 1
fi
