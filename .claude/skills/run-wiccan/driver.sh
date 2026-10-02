#!/usr/bin/env bash
# Wiccan driver: build the app, render its UI to PNG, and drive the backend.
#
# Run from the repo root:  .claude/skills/run-wiccan/driver.sh <command>
#
#   deps         install the Android SDK + Firebase CLI this container needs
#   build        assemble the prod debug APK
#   screenshots  render all six palettes to PNG (no emulator, no device)
#   backend      start the Firebase emulators and smoke-test the callables
#   test         the full JVM check matrix (both toolchains)
#   all          build + screenshots + backend
#
# There is no Android emulator here (no /dev/kvm), so `screenshots` is the
# only way to actually see the UI. It renders Compose on the JVM through
# Robolectric + Roborazzi.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$ROOT"

# An existing local.properties wins, so re-running `deps` never installs a
# second copy of the ~760MB SDK next to one that already works.
SDK_DIR=""
if [ -f local.properties ]; then
  SDK_DIR="$(sed -n 's/^sdk\.dir=//p' local.properties | head -1)"
fi
SDK_DIR="${SDK_DIR:-${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/android-sdk}}}"
SHOTS="app/build/outputs/roborazzi"
EMU_LOG="${TMPDIR:-/tmp}/wiccan-emulators.log"
PROJECT="wiccan-android"

log() { printf '\n=== %s ===\n' "$*"; }

cmd_deps() {
  if [ ! -d "$SDK_DIR/platforms/android-37.0" ] || [ ! -d "$SDK_DIR/build-tools/37.0.0" ]; then
    log "Installing Android SDK into $SDK_DIR"
    # Reuse an installed manager when upgrading an existing SDK, rather
    # than nesting a second cmdline-tools directory under latest.
    if [ ! -x "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" ]; then
      mkdir -p "$SDK_DIR/cmdline-tools"
      local zip="$SDK_DIR/cmdtools.zip"
      curl -fsSL -o "$zip" \
        https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip
      unzip -q "$zip" -d "$SDK_DIR/cmdline-tools"
      mv "$SDK_DIR/cmdline-tools/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
      rm -f "$zip"
    fi
    yes | "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null 2>&1 || true
    "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK_DIR" \
      "platform-tools" "platforms;android-37.0" "build-tools;37.0.0" >/dev/null
  fi
  grep -q '^sdk.dir=' local.properties 2>/dev/null || printf '\nsdk.dir=%s\n' "$SDK_DIR" >> local.properties
  command -v firebase >/dev/null || npm install -g firebase-tools >/dev/null
  [ -d functions/node_modules ] || (cd functions && npm install >/dev/null)
  log "deps ready"
  echo "  sdk.dir=$SDK_DIR"
  echo "  firebase $(firebase --version)"
}

cmd_build() {
  log "assembleProdDebug"
  ./gradlew :app:assembleProdDebug
  ls -la app/build/outputs/apk/prod/debug/*.apk
}

cmd_screenshots() {
  log "Rendering all six palettes (Robolectric + Roborazzi, JVM only)"
  ./gradlew :app:testProdDebugUnitTest --tests '*WiccanPaletteScreenshotTest*'
  ls -la "$SHOTS"
  echo
  echo "Open these to review color/contrast:"
  for f in "$SHOTS"/wiccan-palette-*.png; do echo "  $f"; done
}

cmd_backend() {
  log "Building functions"
  (cd functions && npm run build >/dev/null)

  log "Starting emulators (ANDROID_PACKAGE_NAME is required - see SKILL.md)"
  pkill -f "emulator[s]:start" >/dev/null 2>&1 || true
  sleep 3
  ANDROID_PACKAGE_NAME=com.jaimesorrow.wiccan \
    nohup firebase emulators:start --only auth,functions,firestore,pubsub \
    --project "$PROJECT" > "$EMU_LOG" 2>&1 &

  local waited=0
  until grep -q "All emulators ready" "$EMU_LOG" 2>/dev/null; do
    sleep 3; waited=$((waited + 3))
    if [ "$waited" -ge 180 ]; then
      echo "Emulators did not come up in ${waited}s. Log tail:" >&2
      tail -20 "$EMU_LOG" >&2; return 1
    fi
  done
  echo "Emulators ready after ${waited}s ($(grep -c 'function initialized' "$EMU_LOG") functions)"

  log "Smoke-testing a callable"
  local base="http://127.0.0.1:5001/$PROJECT/us-central1"
  local body
  body=$(curl -s -X POST "$base/getTodayContent" \
    -H "Content-Type: application/json" -d '{"data":{}}')
  echo "  getTodayContent (no auth) -> $body"
  case "$body" in
    *UNAUTHENTICATED*) echo "  OK: the callable is live and rejecting unattested calls" ;;
    *) echo "  UNEXPECTED: wanted UNAUTHENTICATED, got the above" >&2; return 1 ;;
  esac

  echo
  echo "Emulator UI: http://127.0.0.1:4000   log: $EMU_LOG"
  echo "Stop with:   pkill -f 'emulator[s]:start'"
}

cmd_test() {
  log "Android"
  ./gradlew ktlintCheck :app:testProdDebugUnitTest :app:assembleProdDebug \
    :app:compileProdDebugAndroidTestKotlin :app:lintProdRelease :app:bundleProdRelease
  ./scripts/check-16kb-alignment.sh
  bash scripts/release-preflight.sh
  log "Functions"
  (cd functions && npm run build && npm test && npm run lint)
}

case "${1:-}" in
  deps)        cmd_deps ;;
  build)       cmd_build ;;
  screenshots) cmd_screenshots ;;
  backend)     cmd_backend ;;
  test)        cmd_test ;;
  all)         cmd_build; cmd_screenshots; cmd_backend ;;
  *) sed -n '2,14p' "${BASH_SOURCE[0]}"; exit 1 ;;
esac
