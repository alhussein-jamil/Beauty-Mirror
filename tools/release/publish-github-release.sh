#!/usr/bin/env bash
# Build APKs locally and publish/update a GitHub Release with OTA version.json.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

TOKEN_FILE="$ROOT/tools/ota/token.local"
if [[ "${BM_USE_OTA_TOKEN:-}" == "1" && -f "$TOKEN_FILE" ]]; then
  export GH_TOKEN="$(tr -d '[:space:]' < "$TOKEN_FILE")"
fi

VERSION_NAME="$(python3 - <<'PY'
from pathlib import Path
import re
text = Path("app/build.gradle.kts").read_text()
print(re.search(r'versionName\s*=\s*"([^"]+)"', text).group(1))
PY
)"
VERSION_CODE="$(python3 - <<'PY'
from pathlib import Path
import re
text = Path("app/build.gradle.kts").read_text()
print(re.search(r'versionCode\s*=\s*(\d+)', text).group(1))
PY
)"
TAG="v${VERSION_NAME}"
NOTES_FILE="${1:-}"

echo "Building APKs for ${TAG} (versionCode=${VERSION_CODE})..."
./tools/gradle-run.sh --no-daemon :app:assembleDebug :app:assembleRelease
./tools/copy-apks.sh all

chmod +x ./tools/release/write-release-meta.sh
./tools/release/write-release-meta.sh releases/beauty-mirror-debug.apk releases

ASSETS=(
  releases/beauty-mirror-debug.apk
  releases/version.json
)
[[ -f releases/beauty-mirror-release-unsigned.apk ]] && ASSETS+=(releases/beauty-mirror-release-unsigned.apk)
[[ -f releases/beauty-mirror-release.apk ]] && ASSETS+=(releases/beauty-mirror-release.apk)

TITLE="Beauty Mirror ${VERSION_NAME}"
DEFAULT_NOTES="RELEASE_NOTES_${VERSION_NAME}.md"
if [[ -n "$NOTES_FILE" && -f "$NOTES_FILE" ]]; then
  NOTES_ARGS=(--notes-file "$NOTES_FILE")
elif [[ -f "$DEFAULT_NOTES" ]]; then
  NOTES_ARGS=(--notes-file "$DEFAULT_NOTES")
else
  NOTES_ARGS=(--generate-notes)
fi

if gh release view "$TAG" >/dev/null 2>&1; then
  echo "Updating existing release ${TAG}..."
  gh release upload "$TAG" "${ASSETS[@]}" --clobber
else
  echo "Creating release ${TAG}..."
  gh release create "$TAG" "${ASSETS[@]}" \
    --title "$TITLE" \
    "${NOTES_ARGS[@]}"
fi

echo "Release: $(gh release view "$TAG" --json url -q .url)"
