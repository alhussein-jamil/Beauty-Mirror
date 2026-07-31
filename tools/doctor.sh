#!/usr/bin/env bash
set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE_HOME_PATH="${GRADLE_USER_HOME:-${HOME}/.gradle}"
status=0

export BEAUTY_MIRROR_ALLOW_MISSING_SDK=1
# shellcheck source=android-env.sh
source "$ROOT_DIR/tools/android-env.sh"
SDK_PATH="${ANDROID_HOME:-}"

printf 'Repository: %s\n' "$ROOT_DIR"
printf 'User: %s\n' "$(id -un)"
printf 'Home: %s\n' "$HOME"
printf 'GRADLE_USER_HOME: %s\n' "$GRADLE_HOME_PATH"
printf 'Android SDK: %s\n\n' "${SDK_PATH:-not found}"

printf '%s\n' '=== Disk space ==='
df -h "$HOME" "$ROOT_DIR" 2>/dev/null | awk '!seen[$1]++'
printf '\n%s\n' '=== Inodes ==='
df -i "$HOME" "$ROOT_DIR" 2>/dev/null | awk '!seen[$1]++'

printf '\n%s\n' '=== Java ==='
if command -v java >/dev/null 2>&1; then
  java -version 2>&1 | head -n 3
  java_major=$(java -version 2>&1 | awk -F'"' '/version/ {split($2,a,"."); print (a[1] == 1 ? a[2] : a[1]); exit}')
  if [[ -z "$java_major" || "$java_major" -lt 17 ]]; then
    printf 'ERROR: Java 17 or newer is required; detected Java %s.\n' "${java_major:-unknown}" >&2
    status=1
  fi
else
  printf 'ERROR: java is not installed or not on PATH.\n' >&2
  status=1
fi

printf '\n%s\n' '=== Gradle cache ==='
if [[ -L "$GRADLE_HOME_PATH" && ! -e "$GRADLE_HOME_PATH" ]]; then
  printf 'ERROR: %s is a broken symlink.\n' "$GRADLE_HOME_PATH" >&2
  status=1
elif [[ -e "$GRADLE_HOME_PATH" && ! -d "$GRADLE_HOME_PATH" ]]; then
  printf 'ERROR: %s exists but is not a directory.\n' "$GRADLE_HOME_PATH" >&2
  status=1
elif mkdir -p "$GRADLE_HOME_PATH/wrapper/dists" 2>/dev/null; then
  if [[ -w "$GRADLE_HOME_PATH" ]]; then
    printf 'OK: Gradle cache is writable.\n'
  else
    printf 'ERROR: Gradle cache is not writable.\n' >&2
    status=1
  fi
else
  printf 'ERROR: cannot create %s/wrapper/dists.\n' "$GRADLE_HOME_PATH" >&2
  status=1
fi

printf '\n%s\n' '=== Android SDK ==='
if [[ -L "$HOME/Android/Sdk" && ! -e "$HOME/Android/Sdk" ]]; then
  printf 'ERROR: %s is a broken symlink to %s.\n' \
    "$HOME/Android/Sdk" "$(readlink "$HOME/Android/Sdk")" >&2
  printf 'Fix: rm "%s" && mkdir -p "%s"\n' \
    "$HOME/Android/Sdk" "$HOME/Android/Sdk" >&2
  status=1
fi
if [[ -z "$SDK_PATH" || ! -d "$SDK_PATH" ]]; then
  printf 'ERROR: Android SDK not found.\n' >&2
  status=1
else
  printf 'OK: SDK directory exists.\n'
  for required in \
    "$SDK_PATH/platforms/android-35/android.jar" \
    "$SDK_PATH/build-tools/35.0.0/aapt2" \
    "$SDK_PATH/platform-tools/adb"; do
    if [[ -e "$required" ]]; then
      printf 'OK: %s\n' "$required"
    else
      printf 'ERROR: missing %s\n' "$required" >&2
      status=1
    fi
  done
fi

printf '\n%s\n' '=== Devices ==='
if [[ -n "$SDK_PATH" && -x "$SDK_PATH/platform-tools/adb" ]]; then
  "$SDK_PATH/platform-tools/adb" devices 2>/dev/null || true
else
  printf 'adb unavailable.\n'
fi

exit "$status"
