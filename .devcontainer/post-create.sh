#!/usr/bin/env bash
set -euo pipefail

sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/opt/android-sdk}}"
gradle_home="${GRADLE_USER_HOME:-/home/vscode/.gradle}"
sdkmanager_path="${sdk_root}/cmdline-tools/latest/bin/sdkmanager"

if [[ ! -x "${sdkmanager_path}" ]]; then
  printf 'Android sdkmanager was not found at %s.\n' "${sdkmanager_path}" >&2
  exit 1
fi

mkdir -p "${gradle_home}"

if [[ ! -f local.properties ]]; then
  printf 'sdk.dir=%s\n' "${sdk_root}" > local.properties
fi

chmod +x ./gradlew

for package_dir in functions firestore-tests; do
  if [[ -f "${package_dir}/package-lock.json" ]]; then
    npm --prefix "${package_dir}" ci
  fi
done

python3 scripts/check-toolchain.py

printf '\nWiccan Codespace ready. Start with:\n'
printf '  Java:   %s\n' "$(java -version 2>&1 | sed -n '1p')"
printf '  Node:   %s\n' "$(node --version)"
printf '  Android SDK tools: %s\n\n' "$("${sdkmanager_path}" --version | sed -n '1p')"
printf '  ./gradlew :app:assembleProdDebug\n'
printf '  ./gradlew :app:testProdDebugUnitTest\n'
printf '  ./gradlew :app:bundleProdRelease\n'
