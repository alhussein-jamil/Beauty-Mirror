#!/usr/bin/env bash
# shellcheck shell=bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

bm_find_sdk() {
  local local_sdk=""
  if [[ -f "$ROOT_DIR/local.properties" ]]; then
    local_sdk="$(sed -n 's/^sdk\.dir=//p' "$ROOT_DIR/local.properties" | tail -n 1)"
  fi

  local candidates=(
    "${ANDROID_HOME:-}"
    "${ANDROID_SDK_ROOT:-}"
    "$local_sdk"
    "$HOME/Android/Sdk"
    "$HOME/Android/sdk"
    "/opt/android-sdk"
    "/usr/local/lib/android/sdk"
  )

  local candidate
  for candidate in "${candidates[@]}"; do
    [[ -n "$candidate" ]] || continue
    if [[ -d "$candidate" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  return 1
}

SDK_PATH="$(bm_find_sdk || true)"
if [[ -z "$SDK_PATH" ]]; then
  if [[ "${BEAUTY_MIRROR_ALLOW_MISSING_SDK:-0}" == "1" ]]; then
    return 0 2>/dev/null || exit 0
  fi
  if [[ -L "$HOME/Android/Sdk" && ! -e "$HOME/Android/Sdk" ]]; then
    printf 'Android SDK path is a broken symlink: %s -> %s\n' \
      "$HOME/Android/Sdk" "$(readlink "$HOME/Android/Sdk")" >&2
    printf 'Repair it with: rm "%s" && mkdir -p "%s"\n\n' \
      "$HOME/Android/Sdk" "$HOME/Android/Sdk" >&2
  fi
  cat >&2 <<'MSG'
Android SDK not found.
Expected one of:
  $ANDROID_HOME
  $ANDROID_SDK_ROOT
  sdk.dir in local.properties
  ~/Android/Sdk

Install Android platform 35 and build-tools 35.0.0, then run `make doctor`.
MSG
  return 1 2>/dev/null || exit 1
fi

export ANDROID_HOME="$SDK_PATH"
export ANDROID_SDK_ROOT="$SDK_PATH"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$ROOT_DIR/local.properties"

if [[ "${1:-}" == "--print" ]]; then
  printf '%s\n' "$ANDROID_HOME"
fi
