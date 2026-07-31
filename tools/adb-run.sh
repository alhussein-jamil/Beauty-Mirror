#!/usr/bin/env bash
set -Eeuo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=android-env.sh
source "$ROOT_DIR/tools/android-env.sh"
ADB="$ANDROID_HOME/platform-tools/adb"
[[ -x "$ADB" ]] || { echo "adb not found at $ADB" >&2; exit 1; }
exec "$ADB" "$@"
