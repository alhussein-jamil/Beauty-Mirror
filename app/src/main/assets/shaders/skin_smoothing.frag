#version 300 es
precision mediump float;
in vec2 vTexCoord;
uniform sampler2D uInput;
uniform sampler2D uSkinMask;
uniform sampler2D uDetailMask;
uniform float uStrength;
uniform float uRadius;
uniform float uDetailRetention;
uniform float uComplexionEvenness;
uniform float uRednessCorrection;
uniform float uBlemishControl;
uniform vec2 uTexelSize;
uniform float uEnabled;
uniform int uSampleCount;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);
vec3 sampleRgb(vec2 uv) { return texture(uInput, clamp(uv, 0.0, 1.0)).rgb; }

vec3 bilateralApprox(vec2 uv, float radius, int samples, float edgeTightness) {
    vec3 center = sampleRgb(uv);
    float centerL = dot(center, LUMA);
    vec3 acc = center * 1.4;
    float wsum = 1.4;
    vec2 o[12];
    o[0] = vec2(radius, 0.0); o[1] = vec2(-radius, 0.0);
    o[2] = vec2(0.0, radius); o[3] = vec2(0.0, -radius);
    o[4] = vec2(radius, radius); o[5] = vec2(-radius, radius);
    o[6] = vec2(radius, -radius); o[7] = vec2(-radius, -radius);
    o[8] = vec2(radius * 1.75, 0.0); o[9] = vec2(-radius * 1.75, 0.0);
    o[10] = vec2(0.0, radius * 1.75); o[11] = vec2(0.0, -radius * 1.75);
    int n = clamp(samples, 4, 12);
    for (int i = 0; i < 12; i++) {
        if (i >= n) break;
        vec2 p = uv + o[i] * uTexelSize;
        vec3 c = sampleRgb(p);
        float l = dot(c, LUMA);
        float wl = exp(-abs(l - centerL) * edgeTightness);
        float skinW = texture(uSkinMask, clamp(p, 0.0, 1.0)).r;
        float w = wl * mix(0.08, 1.0, skinW);
        acc += c * w;
        wsum += w;
    }
    return acc / max(wsum, 1e-4);
}

vec3 preserveLuma(vec3 candidate, float targetLuma) {
    return clamp(candidate + vec3(targetLuma - dot(candidate, LUMA)), 0.0, 1.0);
}

void main() {
    vec4 original = texture(uInput, vTexCoord);
    float skin = texture(uSkinMask, vTexCoord).r;
    float effectAmount = max(max(uStrength, uBlemishControl), max(uComplexionEvenness, uRednessCorrection));
    if (uEnabled < 0.5 || effectAmount < 0.001 || skin < 0.001) {
        fragColor = original;
        return;
    }

    float detailProtect = texture(uDetailMask, vTexCoord).r;
    float protectedSkin = skin * (1.0 - detailProtect * 0.985);
    float radius = max(uRadius, 1.0);

    // Near bilateral always; wide scale only when complexion evenness needs it (GPU save).
    vec3 lowNear = bilateralApprox(vTexCoord, radius, uSampleCount, 15.0);
    vec3 lowWide = lowNear;
    float wideMix = 0.0;
    if (uComplexionEvenness > 0.02) {
        lowWide = bilateralApprox(vTexCoord, radius * 2.15, min(uSampleCount, 8), 10.0);
        wideMix = 0.28 + clamp(uComplexionEvenness, 0.0, 1.0) * 0.34;
    }
    vec3 low = mix(lowNear, lowWide, wideMix);
    vec3 high = original.rgb - lowNear;

    // Detail retention remains intuitive: 100% keeps pores, 0% produces a stronger soft-focus
    // result. Even at high retention the low-frequency complexion is visibly improved.
    float retainedDetail = mix(0.16, 0.86, clamp(uDetailRetention, 0.0, 1.0));
    vec3 softened = low + high * retainedDetail;
    float smoothResponse = pow(clamp(uStrength, 0.0, 1.0), 0.72);
    float smoothAmount = clamp(protectedSkin * smoothResponse * 1.12, 0.0, 0.92);
    vec3 color = mix(original.rgb, softened, smoothAmount);

    // Equalize broad chroma/luminance patches without flattening the face into one color.
    float evenAmount = clamp(protectedSkin * pow(uComplexionEvenness, 0.78) * 0.78, 0.0, 0.70);
    float currentLuma = dot(color, LUMA);
    vec3 evenCandidate = mix(lowNear, lowWide, 0.60);
    color = preserveLuma(mix(color, evenCandidate, evenAmount), mix(currentLuma, dot(evenCandidate, LUMA), evenAmount * 0.20));

    // Selectively neutralize red excess; protected feature masks keep lips/eyes intact.
    float redExcess = max(color.r - (color.g * 0.72 + color.b * 0.28), 0.0);
    float redCandidate = smoothstep(0.018, 0.17, redExcess);
    float rednessAmount = clamp(protectedSkin * redCandidate * pow(uRednessCorrection, 0.76) * 0.78, 0.0, 0.62);
    vec3 neutralized = vec3(
        color.r - redExcess * 0.58,
        color.g + redExcess * 0.13,
        color.b + redExcess * 0.08
    );
    color = preserveLuma(mix(color, neutralized, rednessAmount), dot(color, LUMA));

    // Spot/blemish softening reacts only where the center differs noticeably from the local
    // bilateral estimate. This keeps normal skin texture while reducing isolated acne and
    // high-contrast marks without a detector model.
    float residual = length(original.rgb - lowNear);
    // Threshold stays above typical pore/stubble energy so only isolated spots blend hard.
    float spotCandidate = smoothstep(0.055, 0.22, residual);
    float spotAmount = clamp(protectedSkin * spotCandidate * pow(uBlemishControl, 0.66) * 0.90, 0.0, 0.86);
    color = mix(color, lowNear, spotAmount);

    fragColor = vec4(clamp(color, 0.0, 1.0), original.a);
}
