#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

canonical="app/src/main/assets/shaders"
required=(
  fullscreen.vert camera_oes.frag mask.vert mask.frag mask_blur.frag
  skin_smoothing.frag under_eye_correction.frag face_lighting.frag
  detail_restoration.frag face_warp.frag feature_enhancement.frag
  color_correction.frag lake_reflection.frag final_composite.frag
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

# Verify every fragment-pass uniform queried by Kotlin exists in its shader source.
python3 - <<'PY_UNIFORMS'
import re
from pathlib import Path

pairs = {
    "CameraInputPass.kt": "camera_oes.frag",
    "ColorCorrectionPass.kt": "color_correction.frag",
    "DetailRestorationPass.kt": "detail_restoration.frag",
    "FaceLightingPass.kt": "face_lighting.frag",
    "FaceWarpPass.kt": "face_warp.frag",
    "FeatureEnhancementPass.kt": "feature_enhancement.frag",
    "FinalCompositePass.kt": "final_composite.frag",
    "LakeReflectionPass.kt": "lake_reflection.frag",
    "SkinSmoothingPass.kt": "skin_smoothing.frag",
    "UnderEyeCorrectionPass.kt": "under_eye_correction.frag",
}
pass_dir = Path("app/src/main/java/com/beautymirror/app/rendering/passes")
shader_dir = Path("app/src/main/assets/shaders")
errors = []
for kotlin_name, shader_name in pairs.items():
    kotlin = (pass_dir / kotlin_name).read_text()
    shader = (shader_dir / shader_name).read_text()
    queried = set(re.findall(r'uniformLocation\("([A-Za-z_]\w*)"\)', kotlin))
    declared = set(re.findall(r'\buniform\s+\w+\s+([A-Za-z_]\w*)\s*;', shader))
    missing = sorted(queried - declared)
    if missing:
        errors.append(f"{kotlin_name} queries missing uniforms in {shader_name}: {', '.join(missing)}")
if errors:
    raise SystemExit("\n".join(errors))
PY_UNIFORMS

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

# INTERNET is allowed for GitHub OTA only. ACCESS_NETWORK_STATE must stay removed.
if grep -R --line-number --include='AndroidManifest.xml' \
  -E 'android.permission.ACCESS_NETWORK_STATE' app/src/main | grep -v 'tools:node="remove"'; then
  echo "ACCESS_NETWORK_STATE must be stripped in source manifest" >&2
  exit 1
fi
if ! grep -R --include='AndroidManifest.xml' -E 'android.permission.INTERNET' app/src/main \
  | grep -v 'tools:node="remove"' | grep -q .; then
  echo "INTERNET permission required for GitHub OTA" >&2
  exit 1
fi

if grep -R --line-number -E 'signingConfigs\.getByName\("debug"\)|signingConfig[[:space:]]*=[[:space:]]*signingConfigs\.debug' \
  --include='*.gradle' --include='*.gradle.kts' .; then
  echo "Release configuration references debug signing" >&2
  exit 1
fi

if grep -R --line-number --include='*.kt' \
  -E '^import androidx\.compose\.foundation\.layout\.(weight|matchParentSize)$' app/src/main/java; then
  echo "Do not directly import scoped Compose layout extensions (weight/matchParentSize)." >&2
  exit 1
fi

echo "Static checks passed"
