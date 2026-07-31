#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p releases

mode="${1:-all}"

copy_debug() {
  local source="app/build/outputs/apk/debug/app-debug.apk"
  [[ -f "$source" ]] || { echo "Missing debug APK: $source" >&2; return 1; }
  cp -f "$source" releases/beauty-mirror-debug.apk
  echo "APK: $(pwd)/releases/beauty-mirror-debug.apk"
}

copy_release() {
  local source
  source=$(find app/build/outputs/apk/release -maxdepth 1 -type f -name '*.apk' -print | sort | head -n 1 || true)
  [[ -n "$source" ]] || { echo "Missing release APK" >&2; return 1; }
  local destination=beauty-mirror-release.apk
  if [[ "$(basename "$source")" == *unsigned* ]]; then
    destination=beauty-mirror-release-unsigned.apk
  fi
  cp -f "$source" "releases/$destination"
  echo "APK: $(pwd)/releases/$destination"
}

case "$mode" in
  debug) copy_debug ;;
  release) copy_release ;;
  all) copy_debug; copy_release ;;
  *) echo "Usage: $0 [debug|release|all]" >&2; exit 2 ;;
esac
