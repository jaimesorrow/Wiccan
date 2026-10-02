#!/usr/bin/env bash
# Print every certificate fingerprint this project has to register, from
# whichever signing material you actually have on hand.
#
#   scripts/signing-fingerprints.sh                      the debug keystore
#   scripts/signing-fingerprints.sh --keystore up.jks --alias wiccan-upload
#   scripts/signing-fingerprints.sh --apk  app/build/outputs/apk/prod/debug/app-prod-debug.apk
#   scripts/signing-fingerprints.sh --aab  app/build/outputs/bundle/prodRelease/app-prod-release.aab
#
# Three different certificates sign this app over its life, and Firebase
# needs all three registered or Google sign-in and App Check fail:
#
#   1. debug      ~/.android/debug.keystore, per developer machine and per
#                 CI container - a new machine means a new fingerprint.
#   2. upload     the key you create once and keep; Play verifies uploads
#                 against it. NOT the key users' devices see.
#   3. app-signing  Google's own key, generated when the first bundle is
#                 uploaded under Play App Signing. This is what ends up on
#                 devices, so it is the one production actually depends on.
#                 It cannot be derived from anything local - copy it from
#                 Play Console - Test and release - Setup - App signing.
#
# --apk/--aab read the certificate out of a built artifact rather than a
# keystore, which is how you confirm what a bundle was *actually* signed
# with instead of what it was meant to be signed with.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

MODE=keystore
STORE="${HOME}/.android/debug.keystore"
ALIAS=androiddebugkey
STOREPASS=android
ARTIFACT=""
LABEL="debug keystore"

while [ $# -gt 0 ]; do
  case "$1" in
    --keystore) MODE=keystore; STORE="$2"; ALIAS=""; STOREPASS=""; LABEL="keystore $2"; shift 2 ;;
    --alias)    ALIAS="$2"; shift 2 ;;
    --apk)      MODE=artifact; ARTIFACT="$2"; LABEL="APK $2"; shift 2 ;;
    --aab)      MODE=artifact; ARTIFACT="$2"; LABEL="bundle $2"; shift 2 ;;
    -h|--help)  sed -n '2,30p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *)          echo "Unknown argument: $1 (try --help)" >&2; exit 2 ;;
  esac
done

printf 'Package     com.jaimesorrow.wiccan\n'
printf 'Firebase    wiccan-android\n'
printf 'Source      %s\n\n' "$LABEL"

if [ "$MODE" = keystore ]; then
  [ -f "$STORE" ] || { echo "No keystore at $STORE" >&2; exit 1; }
  # Prompt rather than take a password on the command line: an argv
  # password lands in the shell history and in every process listing.
  if [ -z "$STOREPASS" ]; then
    # Prompt on stderr so the fingerprint report on stdout stays pipeable.
    printf 'Store password for %s: ' "$(basename "$STORE")" >&2
    read -r -s STOREPASS; printf '\n' >&2
  fi
  if [ -z "$ALIAS" ]; then
    ALIAS=$(keytool -list -keystore "$STORE" -storepass "$STOREPASS" 2>/dev/null \
            | sed -n 's/^\([^,]*\), .*, PrivateKeyEntry.*/\1/p' | head -1)
    [ -n "$ALIAS" ] || { echo "Could not read any key alias - wrong password?" >&2; exit 1; }
    printf 'Alias       %s (auto-detected)\n\n' "$ALIAS"
  fi
  out=$(keytool -list -v -keystore "$STORE" -alias "$ALIAS" -storepass "$STOREPASS" 2>/dev/null)
  [ -n "$out" ] || { echo "keytool could not read alias '$ALIAS' from $STORE" >&2; exit 1; }
  printf '%s\n' "$out" | grep -E '^(Owner|Valid from):' | sed 's/^/  /'
  printf '%s\n' "$out" | grep -E '^[[:space:]]*SHA(1|256):' | sed 's/^[[:space:]]*/  /'
else
  [ -f "$ARTIFACT" ] || { echo "No such artifact: $ARTIFACT" >&2; exit 1; }
  # An .aab is not signed the way an .apk is, so apksigner cannot read it;
  # fall back to pulling the signature block out of the zip directly.
  case "$ARTIFACT" in
    *.apk)
      sdk=$(sed -n 's/^sdk\.dir=//p' local.properties 2>/dev/null | head -1)
      sdk="${sdk:-${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}}"
      signer=$(ls "$sdk"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1)
      [ -n "$signer" ] || { echo "apksigner not found under $sdk/build-tools" >&2; exit 1; }
      "$signer" verify --print-certs "$ARTIFACT" 2>/dev/null \
        | grep -iE 'certificate (DN|SHA-1|SHA-256)' | sed 's/^/  /' \
        || { echo "  UNSIGNED - apksigner found no signature" >&2; exit 1; }
      ;;
    *.aab)
      if ! unzip -l "$ARTIFACT" | grep -qiE 'META-INF/.*\.(RSA|DSA|EC)$'; then
        printf '  UNSIGNED - no META-INF/*.RSA signature block.\n'
        printf '  Expected when app/keystore.properties is absent; release.yml signs on a runner.\n'
        exit 1
      fi
      tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT
      unzip -qo "$ARTIFACT" 'META-INF/*' -d "$tmp"
      for blk in "$tmp"/META-INF/*.RSA "$tmp"/META-INF/*.DSA "$tmp"/META-INF/*.EC; do
        [ -f "$blk" ] || continue
        printf '  signature block %s\n' "$(basename "$blk")"
        keytool -printcert -file "$blk" 2>/dev/null \
          | grep -E '^(Owner|Valid from)|SHA(1|256):' | sed 's/^[[:space:]]*/    /'
      done
      ;;
    *) echo "Expected a .apk or .aab" >&2; exit 2 ;;
  esac
fi

cat <<'NOTE'

Register the SHA-1 (and SHA-256, required by App Check / Play Integrity) at
Firebase console - Project settings - Your apps - com.jaimesorrow.wiccan -
Add fingerprint. Re-download google-services.json afterwards: enabling
Google sign-in is what fills in the currently-empty oauth_client array that
default_web_client_id comes from.

Still missing from any local keystore: the Play App Signing certificate.
Copy its SHA-1 and SHA-256 from Play Console after the first upload and add
those too, or production sign-in breaks while debug builds keep working.
NOTE
