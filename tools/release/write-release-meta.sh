#!/usr/bin/env bash
# Write Bokko-compatible version.json for OTA (APK versionCode + checksum).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
APK="${1:?usage: write-release-meta.sh APK_PATH OUTPUT_DIR}"
OUT_DIR="${2:?usage: write-release-meta.sh APK_PATH OUTPUT_DIR}"

if [[ ! -f "$APK" ]]; then
  echo "APK not found: $APK" >&2
  exit 1
fi

# Prefer gradle versionName (never the debug APK "-debug" suffix) so OTA UI stays clean.
# Prefer aapt versionCode when available so meta matches the shipped binary exactly.
BUILD_NUMBER=""
VERSION_NAME="$(python3 - <<'PY'
import re
from pathlib import Path
text = Path("app/build.gradle.kts").read_text()
print(re.search(r'versionName\s*=\s*"([^"]+)"', text).group(1))
PY
)"
if [[ -n "${ANDROID_HOME:-}" && -d "${ANDROID_HOME}/build-tools" ]]; then
  BT="${ANDROID_HOME}/build-tools/$(ls "${ANDROID_HOME}/build-tools" | sort -V | tail -1)"
  BADGING="$("$BT/aapt2" dump badging "$APK" 2>/dev/null || true)"
  BUILD_NUMBER="$(printf '%s\n' "$BADGING" | sed -n "s/.*versionCode='\([0-9]*\)'.*/\1/p" | head -1)"
fi
if [[ -z "$BUILD_NUMBER" ]]; then
  BUILD_NUMBER="$(python3 - <<'PY'
import re
from pathlib import Path
text = Path("app/build.gradle.kts").read_text()
print(re.search(r'versionCode\s*=\s*(\d+)', text).group(1))
PY
)"
fi
# Sanity: APK versionCode must match gradle when both are known.
GRADLE_CODE="$(python3 - <<'PY'
import re
from pathlib import Path
text = Path("app/build.gradle.kts").read_text()
print(re.search(r'versionCode\s*=\s*(\d+)', text).group(1))
PY
)"
if [[ "$BUILD_NUMBER" != "$GRADLE_CODE" ]]; then
  echo "versionCode mismatch: apk/meta=$BUILD_NUMBER gradle=$GRADLE_CODE" >&2
  exit 1
fi

APK_NAME="$(basename "$APK")"
APK_SHA="$(sha256sum "$APK" | awk '{print $1}')"
APK_SIZE="$(stat -c%s "$APK")"
mkdir -p "$OUT_DIR"
python3 - "$OUT_DIR/version.json" "$VERSION_NAME" "$BUILD_NUMBER" "$APK_SHA" "$APK_SIZE" "$APK_NAME" <<'PY'
import json, sys
from datetime import datetime, timezone
out, ver, build, apk_sha, apk_size, apk_name = sys.argv[1:7]
meta = {
    "version": ver,
    "buildNumber": int(build),
    "sha256": apk_sha,
    "size": int(apk_size),
    "apkAssetName": apk_name,
    "publishedAt": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
    "channel": "github-release",
}
with open(out, "w", encoding="utf-8") as f:
    json.dump(meta, f, indent=2)
    f.write("\n")
print(out)
PY
