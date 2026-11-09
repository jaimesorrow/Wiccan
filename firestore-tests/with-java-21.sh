#!/usr/bin/env bash
set -euo pipefail

java_home="${JAVA_HOME:-}"
if [[ -z "${java_home}" ]]; then
  java_path="$(command -v java || true)"
  if [[ -n "${java_path}" ]]; then
    java_home="$(dirname "$(dirname "$(readlink -f "${java_path}")")")"
  fi
fi

java_major=0
if [[ -x "${java_home}/bin/java" ]]; then
  java_major="$("${java_home}/bin/java" -version 2>&1 | sed -nE '1s/.*version "([0-9]+).*/\1/p')"
fi

if [[ ! "${java_major:-0}" -ge 21 ]]; then
  sdkman_candidates="${SDKMAN_DIR:-/usr/local/sdkman}/candidates/java"
  if [[ ! -d "${sdkman_candidates}" && -n "${HOME:-}" ]]; then
    sdkman_candidates="${HOME}/.sdkman/candidates/java"
  fi
  if [[ -d "${sdkman_candidates}" ]]; then
    java_home="$(find "${sdkman_candidates}" -mindepth 1 -maxdepth 1 -type d -name '21.*' -print | sort -V | tail -n 1)"
  else
    java_home=""
  fi
  if [[ -z "${java_home}" ]]; then
    printf 'Java 21 is required for the Firestore emulator tests.\n' >&2
    exit 1
  fi
fi

export JAVA_HOME="${java_home}"
export PATH="${JAVA_HOME}/bin:${PATH}"
exec "$@"
