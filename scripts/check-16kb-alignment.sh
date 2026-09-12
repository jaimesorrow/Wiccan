#!/usr/bin/env bash
# Checks every native library (.so) bundled in a release .aab for 16KB
# (0x4000) page-size alignment on its ELF LOAD segments - required for
# Android 15+ devices with a 16KB memory page size, and enforced by Play
# for new apps/updates targeting recent API levels. Only 64-bit ABIs
# (arm64-v8a, x86_64) are subject to the 16KB requirement; 32-bit ABIs are
# checked too for visibility but never fail the run.
#
# Usage: scripts/check-16kb-alignment.sh [path/to/app-release.aab]
# Defaults to app/build/outputs/bundle/release/app-release.aab (the
# standard `./gradlew :app:bundleRelease` output path).
set -euo pipefail

AAB_PATH="${1:-app/build/outputs/bundle/release/app-release.aab}"
REQUIRED_ALIGNMENT="0x4000"
SIXTY_FOUR_BIT_ABIS="arm64-v8a x86_64"

if [ ! -f "$AAB_PATH" ]; then
  echo "error: $AAB_PATH not found. Run ./gradlew :app:bundleRelease first." >&2
  exit 1
fi

if ! command -v readelf >/dev/null 2>&1; then
  echo "error: readelf not found (part of binutils)." >&2
  exit 1
fi

WORKDIR=$(mktemp -d)
trap 'rm -rf "$WORKDIR"' EXIT

SO_FILES=$(unzip -Z1 "$AAB_PATH" 'base/lib/*.so')
if [ -z "$SO_FILES" ]; then
  echo "No native libraries (.so) found in $AAB_PATH - nothing to check."
  exit 0
fi

FAILED=0
CHECKED=0

while IFS= read -r entry; do
  abi=$(echo "$entry" | cut -d/ -f3)
  name=$(basename "$entry")
  extracted="$WORKDIR/$name"
  unzip -p "$AAB_PATH" "$entry" > "$extracted"

  min_align=$(readelf -lW "$extracted" 2>/dev/null | awk '/LOAD/ {print $NF}' | sort -u | sort -n | head -1)
  CHECKED=$((CHECKED + 1))

  is_64bit=false
  for abi64 in $SIXTY_FOUR_BIT_ABIS; do
    if [ "$abi" = "$abi64" ]; then
      is_64bit=true
    fi
  done

  if [ "$min_align" != "$REQUIRED_ALIGNMENT" ]; then
    if [ "$is_64bit" = true ]; then
      echo "FAIL  $abi/$name: minimum LOAD alignment $min_align (need $REQUIRED_ALIGNMENT)"
      FAILED=$((FAILED + 1))
    else
      echo "warn  $abi/$name: minimum LOAD alignment $min_align (16KB not required on 32-bit ABIs)"
    fi
  else
    echo "ok    $abi/$name: $min_align"
  fi
done <<< "$SO_FILES"

echo ""
echo "Checked $CHECKED native libraries."
if [ "$FAILED" -gt 0 ]; then
  echo "$FAILED failed the 16KB alignment requirement on a 64-bit ABI."
  exit 1
fi
echo "All 64-bit native libraries are 16KB-aligned."
