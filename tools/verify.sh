#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

./tools/static-checks.sh
./tools/gradle-run.sh --no-daemon --stacktrace \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:lintRelease \
  :app:assembleDebug \
  :app:assembleRelease
./tools/verify-built-artifacts.sh
./tools/copy-apks.sh all

echo "Verification passed"
