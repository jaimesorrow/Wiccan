#!/usr/bin/env bash
# One command that answers "is this repo whole, consistent, and buildable?"
#
#   scripts/validate-repo.sh              structure + both build matrices
#   scripts/validate-repo.sh --structure  structure only (seconds, no toolchain)
#
# Three layers, cheapest first, so a structural problem is reported in
# seconds instead of after a six-minute build:
#
#   1. Structure  - invariants that no other check covers: cross-file
#                   identity drift, conflict markers, build output or
#                   secrets committed, referenced files that don't exist.
#   2. Preflight  - delegates to scripts/release-preflight.sh (release
#                   identity + security), rather than restating it here.
#   3. Build      - run the same build commands as CI locally. Hosted
#                   Actions execution must be verified separately.
#
# Layer 1 exists because the expensive layers cannot catch this class of
# problem. A build is perfectly happy to compile a repo whose applicationId
# and google-services.json disagree, or one carrying a conflict marker in a
# doc - those ship as runtime or review failures instead.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

STRUCTURE_ONLY=0
[ "${1:-}" = "--structure" ] && STRUCTURE_ONLY=1

failures=0
pass() { printf '  PASS  %s\n' "$1"; }
fail() { printf '  FAIL  %s\n' "$1"; failures=$((failures + 1)); }
section() { printf '\n%s\n%s\n' "$1" "$(printf '%.0s-' $(seq ${#1}))"; }

# The identity every config file has to agree on. Drift here is silent:
# Play Billing looks purchases up by package name, and App Check and Google
# sign-in both key off the registered client, so a mismatch fails in
# production while every local build stays green.
APP_ID="com.jaimesorrow.wiccan"
NAMESPACE="$APP_ID"

section "1. Structure"

# --- identity consistency across every file that encodes it -------------
if grep -Eq "applicationId = \"${APP_ID//./\\.}\"" app/build.gradle.kts; then
  pass "applicationId is $APP_ID"
else
  fail "applicationId is not $APP_ID"
fi

for gs in app/google-services.ci.json app/src/prod/google-services.json; do
  if [ ! -f "$gs" ]; then
    fail "$gs is missing"
  elif python3 -c "
import json,sys
d=json.load(open('$gs'))
names={c['client_info']['android_client_info']['package_name'] for c in d['client']}
sys.exit(0 if names == {'$APP_ID'} else 1)" 2>/dev/null; then
    pass "$(basename "$gs") registers $APP_ID"
  else
    fail "$(basename "$gs") does not register $APP_ID"
  fi
done

# The backend must keep *requiring* this rather than defaulting it: a
# production-package fallback would make dev/staging billing calls hit the
# production Play listing.
if grep -Eq 'process\.env\.ANDROID_PACKAGE_NAME \?\?' functions/src/index.ts; then
  fail "functions/src/index.ts has a package-name fallback (must fail closed)"
else
  pass "functions requires ANDROID_PACKAGE_NAME with no fallback"
fi

if grep -Eq "namespace = \"${NAMESPACE//./\\.}\"" app/build.gradle.kts; then
  pass "Kotlin namespace is $NAMESPACE (matches applicationId)"
else
  fail "Kotlin namespace is not $NAMESPACE"
fi

# --- nothing half-merged or accidentally committed ----------------------
if git grep -nIE '^(<<<<<<< |>>>>>>> |=======$)' -- . >/dev/null 2>&1; then
  fail "merge conflict markers found in tracked files"
  git grep -nIE '^(<<<<<<< |>>>>>>> |=======$)' -- . | head -5 | sed 's/^/        /'
else
  scan_status=$?
  if (( scan_status == 1 )); then
    pass "no merge conflict markers in tracked files"
  else
    fail "merge conflict marker scan failed (git grep exit $scan_status)"
  fi
fi

stray=$(git ls-files | grep -E '(^|/)(build|lib|node_modules)/|\.(apk|aab|jks|keystore)$|-debug\.log$' || true)
if [ -n "$stray" ]; then
  fail "build output, archives or keystores are tracked"
  echo "$stray" | head -5 | sed 's/^/        /'
else
  pass "no build output, archives or keystores tracked"
fi

# Secrets that must never be committed, regardless of .gitignore working.
if git grep -lIE -- '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----|sk-ant-[A-Za-z0-9_-]{20,}' -- . >/dev/null 2>&1; then
  fail "private key material or an API secret is committed"
else
  scan_status=$?
  if (( scan_status == 1 )); then
    pass "no private key material or API secret committed"
  else
    fail "secret scan failed (git grep exit $scan_status)"
  fi
fi

# --- every referenced file actually exists ------------------------------
# A workflow or skill that names a missing script fails only when someone
# runs it, which for the release workflow means at release time.
missing_refs=0
for ref in $(grep -ohE '(\./)?scripts/[a-zA-Z0-9_.-]+\.(sh|mjs|py)' \
             .github/workflows/*.yml .claude/skills/*/SKILL.md .claude/skills/*/driver.sh 2>/dev/null \
             | sed 's|^\./||' | sort -u); do
  [ -f "$ref" ] || { fail "referenced but missing: $ref"; missing_refs=1; }
done
[ "$missing_refs" -eq 0 ] && pass "every script referenced by workflows/skills exists"

nonexec=0
for s in scripts/*.sh scripts/*.mjs .claude/skills/*/driver.sh; do
  [ -e "$s" ] || continue
  [ -x "$s" ] || { fail "$s has a shebang but is not executable"; nonexec=1; }
done
[ "$nonexec" -eq 0 ] && pass "every script is executable"

# --- flavor configuration matches what the docs promise -----------------
if [ -f app/src/prod/google-services.json ]; then
  pass "prod flavor has its own google-services.json"
else
  fail "prod flavor is missing app/src/prod/google-services.json"
fi
for flavor in dev staging; do
  if [ -f "app/src/$flavor/google-services.json" ]; then
    pass "$flavor flavor has a google-services.json"
  else
    pass "$flavor flavor has no google-services.json (expected; documented)"
  fi
done

section "2. Release preflight"
if bash scripts/release-preflight.sh > /tmp/validate-preflight.$$ 2>&1; then
  pass "release-preflight.sh: $(grep -c '^PASS' /tmp/validate-preflight.$$) checks passed"
else
  fail "release-preflight.sh reported failures"
  grep '^FAIL' /tmp/validate-preflight.$$ | sed 's/^/        /'
fi
rm -f /tmp/validate-preflight.$$

if [ "$STRUCTURE_ONLY" -eq 1 ]; then
  section "Result"
  printf '%d failure(s) - structure only, build matrix skipped\n' "$failures"
  exit $(( failures > 0 ))
fi

section "3. Build - Android"
# google-services.json at the app root is gitignored, so a fresh clone has
# none. The prod flavor reads app/src/prod/ and does not need it, but the
# plugin still wants a root file present for the other flavors.
[ -f app/google-services.json ] || cp app/google-services.ci.json app/google-services.json

# Run tasks in small groups: chaining the whole matrix in one invocation
# has been observed getting OOM-killed (exit 137) on a 4-core/15GB box.
run_gradle() {
  local label="$1"; shift
  local status=0
  if ./gradlew "$@" > /tmp/validate-gradle.$$ 2>&1; then
    pass "$label"
  else
    status=1
    fail "$label"
    grep -E '^e: |FAILED|Error:|What went wrong' /tmp/validate-gradle.$$ | head -5 | sed 's/^/        /'
  fi
  rm -f /tmp/validate-gradle.$$
  return "$status"
}

run_gradle "ktlintCheck"                   ktlintCheck
run_gradle "unit tests (prodDebug)"        :app:testProdDebugUnitTest
run_gradle "assemble prodDebug"            :app:assembleProdDebug
run_gradle "androidTest compiles"          :app:compileProdDebugAndroidTestKotlin
run_gradle "lint prodRelease"              :app:lintProdRelease
if run_gradle "bundle prodRelease" :app:bundleProdRelease; then
  if ./scripts/check-16kb-alignment.sh > /tmp/validate-16kb.$$ 2>&1; then
    pass "16KB alignment on the release bundle"
  else
    fail "16KB alignment"
    tail -5 /tmp/validate-16kb.$$ | sed 's/^/        /'
  fi
  rm -f /tmp/validate-16kb.$$
else
  echo "  SKIP  16KB alignment: release bundle build failed"
fi

section "4. Build - Cloud Functions"
# Match CI's locked install even when an older node_modules directory exists.
if (cd functions && npm ci) > /tmp/validate-fn.$$ 2>&1; then
  pass "functions dependencies (npm ci)"
  rm -f /tmp/validate-fn.$$
  for step in build test lint; do
    if (cd functions && npm run "$step") > /tmp/validate-fn.$$ 2>&1; then
      pass "functions npm run $step"
    else
      fail "functions npm run $step"
      tail -5 /tmp/validate-fn.$$ | sed 's/^/        /'
    fi
    rm -f /tmp/validate-fn.$$
  done
else
  fail "functions npm ci"
  tail -5 /tmp/validate-fn.$$ | sed 's/^/        /'
  rm -f /tmp/validate-fn.$$
  echo "  SKIP  functions build/test/lint: dependency installation failed"
fi

section "Result"
if [ "$failures" -eq 0 ]; then
  printf 'All checks passed. Structure is consistent and both toolchains build.\n'
else
  printf '%d failure(s).\n' "$failures"
fi
exit $(( failures > 0 ))
