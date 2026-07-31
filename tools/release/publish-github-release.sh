#!/usr/bin/env bash
# Build APKs locally and publish/update a GitHub Release.
# Prefers tools/ota/token.local, then GH_TOKEN / existing gh auth.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

# Uses ambient `gh` auth (or an already-exported GH_TOKEN).
# Optional override: BM_USE_OTA_TOKEN=1 loads tools/ota/token.local — PAT must have
# Contents: write (releases). Do not force-load a weak PAT over a working gh session.
TOKEN_FILE="$ROOT/tools/ota/token.local"
if [[ "${BM_USE_OTA_TOKEN:-}" == "1" && -f "$TOKEN_FILE" ]]; then
  export GH_TOKEN="$(tr -d '[:space:]' < "$TOKEN_FILE")"
fi

VERSION_NAME="$(python3 - <<'PY'
from pathlib import Path
import re
text = Path("app/build.gradle.kts").read_text()
m = re.search(r'versionName\s*=\s*"([^"]+)"', text)
print(m.group(1) if m else "0.0.0")
PY
)"
VERSION_CODE="$(python3 - <<'PY'
from pathlib import Path
import re
text = Path("app/build.gradle.kts").read_text()
m = re.search(r'versionCode\s*=\s*(\d+)', text)
print(m.group(1) if m else "0")
PY
)"
TAG="v${VERSION_NAME}"
NOTES_FILE="${1:-}"

echo "Building APKs for ${TAG} (versionCode=${VERSION_CODE})..."
./tools/gradle-run.sh --no-daemon :app:assembleDebug :app:assembleRelease
./tools/copy-apks.sh all

python3 - <<PY
import json
from pathlib import Path
path = Path("releases/version.json")
data = json.loads(path.read_text())
data["version"] = "${VERSION_NAME}"
data["versionCode"] = int("${VERSION_CODE}")
data["channel"] = "github-release"
path.write_text(json.dumps(data, indent=2) + "\n")
print(path)
PY

ASSETS=(
  releases/beauty-mirror-debug.apk
  releases/beauty-mirror-release-unsigned.apk
  releases/version.json
)
[[ -f releases/beauty-mirror-release.apk ]] && ASSETS+=(releases/beauty-mirror-release.apk)

TITLE="Beauty Mirror ${VERSION_NAME}"
if [[ -n "$NOTES_FILE" && -f "$NOTES_FILE" ]]; then
  NOTES_ARGS=(--notes-file "$NOTES_FILE")
elif [[ -f RELEASE_NOTES_3.1.0.md ]]; then
  NOTES_ARGS=(--notes-file RELEASE_NOTES_3.1.0.md)
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
