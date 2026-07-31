#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-${HOME}/.gradle}"

fail() {
  printf 'Gradle bootstrap error: %s\n' "$*" >&2
  exit 1
}

if [[ -L "$GRADLE_USER_HOME" && ! -e "$GRADLE_USER_HOME" ]]; then
  fail "$GRADLE_USER_HOME is a broken symlink. Remove it or point GRADLE_USER_HOME to a writable directory."
fi

if [[ -e "$GRADLE_USER_HOME" && ! -d "$GRADLE_USER_HOME" ]]; then
  fail "$GRADLE_USER_HOME exists but is not a directory."
fi

mkdir -p \
  "$GRADLE_USER_HOME/wrapper/dists" \
  "$GRADLE_USER_HOME/caches" \
  "$GRADLE_USER_HOME/daemon" \
  || fail "cannot create $GRADLE_USER_HOME. Check free disk space and permissions."

[[ -w "$GRADLE_USER_HOME" ]] \
  || fail "$GRADLE_USER_HOME is not writable by $(id -un)."

available_kb=$(df -Pk "$GRADLE_USER_HOME" | awk 'NR==2 {print $4}')
if [[ "${available_kb:-0}" -lt 1048576 ]]; then
  printf 'Warning: less than 1 GiB is free on the Gradle cache filesystem. Android builds may fail.\n' >&2
fi

# Auto-detect the Android SDK and refresh local.properties on every build.
# shellcheck source=android-env.sh
source "$ROOT_DIR/tools/android-env.sh"

exec "$ROOT_DIR/gradlew" "$@"
