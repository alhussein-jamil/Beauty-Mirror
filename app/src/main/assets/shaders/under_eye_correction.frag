#version 300 es
precision highp float;
in vec2 vTexCoord;
uniform sampler2D uInput;
uniform sampler2D uUnderEyeMask;
uniform vec2 uLeftCheekUv;
uniform vec2 uRightCheekUv;
uniform vec3 uLeftCheekRef;
uniform vec3 uRightCheekRef;
uniform float uStrength;
uniform float uMaxLift;
uniform float uColorCorrection;
uniform float uSmoothing;
uniform float uLeftVisibility;
uniform float uRightVisibility;
uniform float uPoseWeight;
uniform float uEnabled;
uniform vec2 uTexelSize;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

vec3 sampleCheek(vec2 uv, vec3 fallback) {
    vec2 dx = vec2(uTexelSize.x * 2.0, 0.0);
    vec2 dy = vec2(0.0, uTexelSize.y * 2.0);
    vec3 a = texture(uInput, clamp(uv, 0.0, 1.0)).rgb;
    vec3 b = texture(uInput, clamp(uv + dx, 0.0, 1.0)).rgb;
    vec3 c = texture(uInput, clamp(uv - dx, 0.0, 1.0)).rgb;
    vec3 d = texture(uInput, clamp(uv + dy, 0.0, 1.0)).rgb;
    vec3 e = texture(uInput, clamp(uv - dy, 0.0, 1.0)).rgb;
    vec3 sampled = (a * 2.0 + b + c + d + e) / 6.0;
    float luma = dot(sampled, LUMA);
    float validity = smoothstep(0.045, 0.16, luma) * (1.0 - smoothstep(0.92, 1.0, luma));
    return mix(fallback, sampled, validity);
}

void main() {
    vec4 original = texture(uInput, vTexCoord);
    if (uEnabled < 0.5 || max(max(uStrength, uSmoothing), max(uColorCorrection, uMaxLift)) < 0.001) {
        fragColor = original;
        return;
    }

    vec2 eyeMask = texture(uUnderEyeMask, vTexCoord).rg;
    eyeMask *= vec2(uLeftVisibility, uRightVisibility) * clamp(uPoseWeight, 0.0, 1.0);
    float mask = max(eyeMask.r, eyeMask.g);
    if (mask < 0.001) {
        fragColor = original;
        return;
    }

    vec3 leftRef = sampleCheek(uLeftCheekUv, uLeftCheekRef);
    vec3 rightRef = sampleCheek(uRightCheekUv, uRightCheekRef);
    float total = max(eyeMask.r + eyeMask.g, 1e-4);
    vec3 reference = (leftRef * eyeMask.r + rightRef * eyeMask.g) / total;

    vec2 sx = vec2(uTexelSize.x * 2.2, 0.0);
    vec2 sy = vec2(0.0, uTexelSize.y * 2.2);
    vec3 localSoft = (
        original.rgb * 2.0 +
        texture(uInput, clamp(vTexCoord + sx, 0.0, 1.0)).rgb +
        texture(uInput, clamp(vTexCoord - sx, 0.0, 1.0)).rgb +
        texture(uInput, clamp(vTexCoord + sy, 0.0, 1.0)).rgb +
        texture(uInput, clamp(vTexCoord - sy, 0.0, 1.0)).rgb
    ) / 6.0;
    vec3 base = mix(original.rgb, localSoft, clamp(mask * uSmoothing * 0.58, 0.0, 0.56));

    float luma = dot(base, LUMA);
    float referenceLuma = dot(reference, LUMA);
    // Compute the bounded target once. Strength is applied only in the final blend, avoiding
    // the previous quadratic response that made low and medium slider values ineffective.
    float lift = clamp(referenceLuma - luma, 0.0, uMaxLift);
    vec3 originalChroma = base - vec3(luma);
    vec3 referenceChroma = reference - vec3(referenceLuma);
    vec3 corrected = vec3(luma + lift) + mix(
        originalChroma,
        referenceChroma,
        clamp(uColorCorrection * 0.42, 0.0, 0.46)
    );

    // Strength drives the main blend; color/lift-only edits still need a floor amount.
    float strengthAmt = pow(clamp(uStrength, 0.0, 1.0), 0.78) * 1.18;
    float auxAmt = max(uColorCorrection, clamp(uMaxLift / 0.4, 0.0, 1.0)) * 0.62;
    float amount = clamp(mask * max(strengthAmt, auxAmt), 0.0, 0.92);
    fragColor = vec4(mix(base, clamp(corrected, 0.0, 1.0), amount), original.a);
}
