#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

canonical="app/src/main/assets/shaders"
required=(
  fullscreen.vert camera_oes.frag mask.vert mask.frag mask_blur.frag
  skin_smoothing.frag under_eye_correction.frag face_lighting.frag
  detail_restoration.frag face_warp.frag feature_enhancement.frag
  color_correction.frag final_composite.frag
)
for shader in "${required[@]}"; do
  test -s "$canonical/$shader" || { echo "Missing shader: $canonical/$shader" >&2; exit 1; }
done

# GLSL ES has additional reserved identifiers that some desktop compilers tolerate.
# Catch them before an APK reaches a physical phone.
python3 - <<'PY_SHADERS'
import re
from pathlib import Path

reserved = {
    "active", "asm", "cast", "class", "common", "enum", "extern", "external",
    "filter", "fixed", "goto", "half", "hvec2", "hvec3", "hvec4", "input",
    "interface", "long", "namespace", "noinline", "output", "packed", "partition",
    "public", "resource", "sampler3DRect", "short", "sizeof", "static", "superp",
    "template", "this", "typedef", "union", "unsigned", "using"
}
identifier_decl = re.compile(
    r"\b(?:float|double|int|uint|bool|vec[234]|dvec[234]|ivec[234]|uvec[234]|bvec[234]|"
    r"mat[234](?:x[234])?|dmat[234](?:x[234])?|sampler[A-Za-z0-9_]*)\s+([A-Za-z_]\w*)"
)
errors = []
for path in sorted(Path("app/src/main/assets/shaders").glob("*")):
    if path.suffix not in {".frag", ".vert"}:
        continue
    for line_no, line in enumerate(path.read_text().splitlines(), 1):
        source = line.split("//", 1)[0]
        for name in identifier_decl.findall(source):
            if name in reserved:
                errors.append(f"{path}:{line_no}: reserved GLSL ES identifier: {name}")
if errors:
    raise SystemExit("\n".join(errors))
PY_SHADERS

test -s app/src/main/assets/face_landmarker.task || {
  echo "Missing MediaPipe model asset" >&2
  exit 1
}
python3 - <<'PY_MODEL'
from hashlib import sha256
from pathlib import Path
expected = "64184e229b263107bc2b804c6625db1341ff2bb731874b0bcc2fe6544e0bc9ff"
actual = sha256(Path("app/src/main/assets/face_landmarker.task").read_bytes()).hexdigest()
if actual != expected:
    raise SystemExit(f"Unexpected face_landmarker.task checksum: {actual}")
PY_MODEL

if find app docs -type f \( -name '*.frag' -o -name '*.vert' \) \
  ! -path "$canonical/*" ! -path 'app/build/*' | grep -q .; then
  echo "Duplicate shader source found outside $canonical" >&2
  find app docs -type f \( -name '*.frag' -o -name '*.vert' \) \
    ! -path "$canonical/*" ! -path 'app/build/*' >&2
  exit 1
fi

if grep -R --line-number --include='AndroidManifest.xml' \
  -E 'android.permission.(INTERNET|ACCESS_NETWORK_STATE)' app/src/main | grep -v 'tools:node="remove"'; then
  echo "Network permission declared in source manifest" >&2
  exit 1
fi

if grep -R --line-number -E 'signingConfigs\.getByName\("debug"\)|signingConfig[[:space:]]*=[[:space:]]*signingConfigs\.debug' \
  --include='*.gradle' --include='*.gradle.kts' .; then
  echo "Release configuration references debug signing" >&2
  exit 1
fi

if grep -R --line-number --include='*.kt' \
  '^import androidx\.compose\.foundation\.layout\.weight$' app/src/main/java; then
  echo "Do not import Compose RowScope/ColumnScope weight directly; use it only inside the scope." >&2
  exit 1
fi

echo "Static checks passed"
