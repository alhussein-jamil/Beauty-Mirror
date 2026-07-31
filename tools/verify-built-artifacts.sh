#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

python3 - <<'PY'
import glob
import pathlib
import sys
import zipfile

required = {
    "assets/face_landmarker.task",
    "assets/shaders/camera_oes.frag",
    "assets/shaders/final_composite.frag",
    "assets/shaders/skin_smoothing.frag",
    "assets/shaders/under_eye_correction.frag",
    "assets/shaders/face_warp.frag",
    "assets/shaders/feature_enhancement.frag",
}
apks = sorted(glob.glob("app/build/outputs/apk/**/*.apk", recursive=True))
if not apks:
    raise SystemExit("No APKs produced")
for apk in apks:
    with zipfile.ZipFile(apk) as archive:
        names = set(archive.namelist())
        missing = sorted(required - names)
        if missing:
            raise SystemExit(f"{apk} missing packaged assets: {missing}")
    print(f"assets OK: {pathlib.Path(apk).name}")
PY

manifest="$(find app/build/intermediates/merged_manifests/release -name AndroidManifest.xml -print -quit)"
if [[ -z "$manifest" ]]; then
  echo "Merged release manifest not found" >&2
  exit 1
fi
if grep -qE 'android.permission.ACCESS_NETWORK_STATE' "$manifest"; then
  echo "ACCESS_NETWORK_STATE present in merged release manifest: $manifest" >&2
  exit 1
fi
if ! grep -qE 'android.permission.INTERNET' "$manifest"; then
  echo "INTERNET missing from merged release manifest (required for OTA): $manifest" >&2
  exit 1
fi

echo "Built artifact checks passed"
