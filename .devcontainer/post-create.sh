#!/usr/bin/env bash
set -euo pipefail

if [[ -n "${ANDROID_HOME:-}" && ! -f local.properties ]]; then
  printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
fi

if [[ -f functions/package-lock.json ]]; then
  npm --prefix functions ci
fi

printf '\nWiccan Codespace ready. Start with:\n'
printf '  ./gradlew :app:assembleProdDebug\n'
printf '  ./gradlew :app:testProdDebugUnitTest\n'
printf '  ./gradlew :app:bundleProdRelease\n'
