#version 300 es
precision highp float;
in vec2 vTexCoord;
uniform sampler2D uInput;
uniform sampler2D uFaceMask;
uniform vec2 uLeftEye;
uniform vec2 uRightEye;
uniform vec2 uLeftJaw;
uniform vec2 uRightJaw;
uniform vec2 uFaceCenter;
uniform vec2 uNoseCenter;
uniform vec2 uNoseLeft;
uniform vec2 uNoseRight;
uniform float uFaceWidth;
uniform float uFaceHeight;
uniform float uLeftVisibility;
uniform float uRightVisibility;
uniform float uPoseWeight;
uniform float uEyeEnlarge;
uniform float uFaceSlim;
uniform float uNoseRefine;
uniform float uOpacity;
uniform float uEnabled;
out vec4 fragColor;

float softDisk(vec2 uv, vec2 center, float radius) {
    float d = length(uv - center) / max(radius, 1e-4);
    float w = 1.0 - smoothstep(0.0, 1.0, d);
    return w * w * (3.0 - 2.0 * w);
}

vec2 moveFeature(vec2 uv, vec2 center, vec2 delta, float radius, float amount) {
    return uv - delta * (softDisk(uv, center, radius) * amount);
}

vec2 enlarge(vec2 uv, vec2 center, float radius, float amount) {
    vec2 d = uv - center;
    float r = length(d);
    if (r >= radius) return uv;
    float w = 1.0 - smoothstep(0.0, radius, r);
    float scale = 1.0 - amount * 0.42 * w * w;
    return center + d * scale;
}

void main() {
    if (uEnabled < 0.5 || uOpacity < 0.001) {
        fragColor = texture(uInput, vTexCoord);
        return;
    }

    float face = texture(uFaceMask, vTexCoord).r;
    float strength = clamp(face * uOpacity * uPoseWeight, 0.0, 1.0);
    vec2 uv = vTexCoord;

    float jawRadius = max(uFaceWidth * 0.34, 0.050);
    vec2 leftJawDelta = normalize(uFaceCenter - uLeftJaw + vec2(1e-5)) * uFaceWidth * 0.10;
    vec2 rightJawDelta = normalize(uFaceCenter - uRightJaw + vec2(1e-5)) * uFaceWidth * 0.10;
    uv = moveFeature(uv, uLeftJaw, leftJawDelta, jawRadius,
        uFaceSlim * strength * uLeftVisibility);
    uv = moveFeature(uv, uRightJaw, rightJawDelta, jawRadius,
        uFaceSlim * strength * uRightVisibility);

    float noseRadius = max(uFaceWidth * 0.18, 0.028);
    vec2 leftNoseDelta = normalize(uNoseCenter - uNoseLeft + vec2(1e-5)) * uFaceWidth * 0.042;
    vec2 rightNoseDelta = normalize(uNoseCenter - uNoseRight + vec2(1e-5)) * uFaceWidth * 0.042;
    uv = moveFeature(uv, uNoseLeft, leftNoseDelta, noseRadius,
        uNoseRefine * strength * uLeftVisibility);
    uv = moveFeature(uv, uNoseRight, rightNoseDelta, noseRadius,
        uNoseRefine * strength * uRightVisibility);

    float eyeRadius = max(uFaceWidth * 0.145, 0.026);
    uv = enlarge(uv, uLeftEye, eyeRadius,
        uEyeEnlarge * strength * uLeftVisibility);
    uv = enlarge(uv, uRightEye, eyeRadius,
        uEyeEnlarge * strength * uRightVisibility);

    fragColor = texture(uInput, clamp(uv, vec2(0.001), vec2(0.999)));
}
