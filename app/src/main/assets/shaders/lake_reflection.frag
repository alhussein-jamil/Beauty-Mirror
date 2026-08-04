#version 300 es
precision highp float;
in vec2 vTexCoord;
uniform sampler2D uInput;
uniform float uTime;
uniform vec2 uViewport;
uniform vec2 uFaceCenter;
uniform vec2 uFaceSize;
uniform float uFacePresence;
uniform float uVisitorReveal;
uniform float uIntensity;
uniform float uMotion;
uniform float uDarkness;
uniform float uFaceClarity;
uniform float uQuality;
uniform float uEnabled;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float softEllipse(vec2 uv, vec2 center, vec2 radius) {
    vec2 d = (uv - center) / max(radius, vec2(0.001));
    return exp(-dot(d, d) * 1.65);
}

float ringProfile(float radius, float centerRadius, float width) {
    float d = (radius - centerRadius) / max(width, 0.001);
    return exp(-d * d);
}

void main() {
    vec4 base = texture(uInput, vTexCoord);
    if (uEnabled < 0.5) {
        fragColor = base;
        return;
    }

    vec2 uv = vTexCoord;
    float aspect = uViewport.x / max(uViewport.y, 1.0);
    vec2 p = uv - vec2(0.5);
    p.x *= aspect;
    float time = uTime;
    float motion = clamp(uMotion, 0.0, 1.0);
    float intensity = clamp(uIntensity, 0.0, 1.0);
    float reveal = clamp(uVisitorReveal, 0.0, 1.0);
    float presence = clamp(uFacePresence, 0.0, 1.0);
    float clarity = clamp(uFaceClarity, 0.0, 1.0);

    // Broad, low-frequency surface flow. The reference pond is mostly still: motion changes
    // phase speed and amplitude, not the number of obvious synthetic rings.
    float phaseA = p.y * 13.0 + sin(p.x * 4.2 + time * 0.055) * 1.25 + time * (0.05 + motion * 0.12);
    float phaseB = dot(p, vec2(8.6, -5.4)) - time * (0.035 + motion * 0.10) + 1.7;
    float phaseC = dot(p, vec2(-4.4, 9.2)) + sin(p.y * 3.0 - time * 0.04) * 0.8;
    float surfaceHeight = sin(phaseA) * 0.50 + sin(phaseB) * 0.31 + sin(phaseC) * 0.19;

    vec2 surfaceSlope;
    surfaceSlope.x =
        cos(phaseA) * cos(p.x * 4.2 + time * 0.055) * 5.25 * 0.50 +
        cos(phaseB) * 8.6 * 0.31 +
        cos(phaseC) * -4.4 * 0.19;
    surfaceSlope.y =
        cos(phaseA) * 13.0 * 0.50 +
        cos(phaseB) * -5.4 * 0.31 +
        cos(phaseC) * (9.2 + cos(p.y * 3.0 - time * 0.04) * 2.4) * 0.19;

    // A single slow arrival ripple is tied to the transformation progress. Two weak ambient
    // ripple sources keep the pond alive without reading as a digital ripple filter.
    vec2 faceVector = uv - uFaceCenter;
    faceVector.x *= aspect;
    float faceRadiusFromCenter = length(faceVector);
    vec2 faceRadial = faceVector / max(faceRadiusFromCenter, 0.0001);
    float arrivalRadius = 0.025 + reveal * 0.82;
    float arrivalWidth = mix(0.018, 0.034, motion);
    float arrivalOuter = ringProfile(faceRadiusFromCenter, arrivalRadius, arrivalWidth);
    float arrivalMiddle = ringProfile(
        faceRadiusFromCenter,
        max(0.0, arrivalRadius - 0.050),
        arrivalWidth * 0.82
    );
    float arrivalInner = ringProfile(
        faceRadiusFromCenter,
        max(0.0, arrivalRadius - 0.095),
        arrivalWidth * 0.72
    );
    // Alternating crests/troughs create a restrained concentric wave train like a real drop in a
    // still pond instead of a single digital displacement ring.
    float arrival = arrivalOuter - arrivalMiddle * 0.52 + arrivalInner * 0.24;
    float arrivalEnvelope = smoothstep(0.015, 0.08, reveal) * (1.0 - smoothstep(0.78, 1.0, reveal));
    surfaceHeight += arrival * arrivalEnvelope * (0.38 + motion * 0.34) * presence;
    surfaceSlope += faceRadial * arrival * arrivalEnvelope * (2.8 + motion * 5.4) * presence;

    vec2 ambientCenterA = vec2(0.24, 0.34);
    vec2 ambientDeltaA = uv - ambientCenterA;
    ambientDeltaA.x *= aspect;
    float ambientRadiusA = length(ambientDeltaA);
    vec2 ambientRadialA = ambientDeltaA / max(ambientRadiusA, 0.0001);
    float ambientCycleA = fract(time * (0.020 + motion * 0.018) + 0.21);
    float ambientRingA = ringProfile(ambientRadiusA, 0.07 + ambientCycleA * 0.48, 0.028 + motion * 0.016);
    surfaceSlope += ambientRadialA * ambientRingA * (1.1 + motion * 2.0);

    vec2 ambientCenterB = vec2(0.78, 0.68);
    vec2 ambientDeltaB = uv - ambientCenterB;
    ambientDeltaB.x *= aspect;
    float ambientRadiusB = length(ambientDeltaB);
    vec2 ambientRadialB = ambientDeltaB / max(ambientRadiusB, 0.0001);
    float ambientCycleB = fract(time * (0.014 + motion * 0.013) + 0.63);
    float ambientRingB = ringProfile(ambientRadiusB, 0.05 + ambientCycleB * 0.40, 0.026 + motion * 0.014);
    float secondAmbientBudget = smoothstep(0.34, 0.58, uQuality);
    surfaceSlope += ambientRadialB * ambientRingB * (0.55 + motion * 1.05) * secondAmbientBudget;

    vec2 faceRadius = max(uFaceSize * vec2(0.64, 0.66), vec2(0.11, 0.15));
    float faceCore = softEllipse(uv, uFaceCenter, faceRadius) * presence;
    float settledFace = faceCore * clarity * smoothstep(0.08, 0.72, reveal);

    // Water remains visible across the face; the reflection only becomes calmer and clearer over
    // time. There is deliberately no hard portal/still ellipse.
    float distortionSuppression = settledFace * 0.78;
    float bend = mix(0.00036, 0.00145, motion) * mix(0.70, 1.0, intensity);
    vec2 distortion = surfaceSlope * bend * (1.0 - distortionSuppression);
    distortion = clamp(distortion, vec2(-0.012), vec2(0.012));
    distortion.x /= aspect;
    vec2 sampleUv = clamp(uv + distortion, vec2(0.002), vec2(0.998));

    vec3 reflected = texture(uInput, sampleUv).rgb;
    if (uQuality > 0.52) {
        vec2 smear = normalize(surfaceSlope + vec2(0.001)) * (0.0008 + motion * 0.0013);
        smear.x /= aspect;
        vec3 sampleA = texture(uInput, clamp(sampleUv + smear, vec2(0.002), vec2(0.998))).rgb;
        vec3 sampleB = texture(uInput, clamp(sampleUv - smear, vec2(0.002), vec2(0.998))).rgb;
        reflected = (reflected * 2.0 + sampleA + sampleB) * 0.25;
    }

    // Murky grey-green pond body inspired by the workshop reference photographs: low saturation,
    // uneven peat patches, soft sky glare and suspended mineral particles.
    float reflectedLuma = dot(reflected, LUMA);
    float broadMurk = 0.5 + 0.5 * sin(p.x * 3.1 + sin(p.y * 2.4 + 0.7) * 1.4);
    broadMurk *= 0.55 + 0.45 * (0.5 + 0.5 * sin(p.y * 4.6 - p.x * 1.8 + 2.2));
    vec3 desaturatedReflection = mix(vec3(reflectedLuma), reflected, 0.34 + settledFace * 0.24);
    vec3 slate = vec3(0.255, 0.275, 0.265);
    vec3 peat = vec3(0.105, 0.120, 0.090);
    vec3 pondBody = mix(peat, slate, 0.30 + broadMurk * 0.48);
    float absorption = 0.22 + clamp(uDarkness, 0.0, 1.0) * 0.38;
    absorption *= 1.0 - settledFace * 0.44;
    vec3 waterColor = mix(desaturatedReflection, pondBody + vec3(reflectedLuma) * 0.16, absorption);

    // Wide horizontal highlights imitate reflected workshop lights / pale sky rather than sharp
    // CGI specular lines.
    float skyBand = pow(max(0.0, sin(uv.y * 15.0 + sin(uv.x * 4.0) * 0.8 + 0.7)), 18.0);
    float slopeGlint = pow(clamp(length(surfaceSlope) * 0.045, 0.0, 1.0), 3.2);
    float crest = 0.5 + 0.5 * surfaceHeight;
    float arrivalCrest = max(arrival, 0.0);
    float glint = (skyBand * 0.045 + slopeGlint * 0.030 + arrivalCrest * arrivalEnvelope * 0.030) *
        (0.42 + intensity * 0.58) * (1.0 - settledFace * 0.58);
    waterColor += vec3(0.73, 0.75, 0.69) * glint;
    waterColor *= 1.0 - (1.0 - crest) * 0.025 * motion;

    // Sparse suspended particles. The grid is static in pond space, avoiding sparkling video noise.
    if (uQuality > 0.28) {
        vec2 particleGrid = uv * vec2(82.0, 138.0);
        vec2 particleCell = floor(particleGrid);
        vec2 particleLocal = fract(particleGrid) - 0.5;
        float particleSeed = hash12(particleCell);
        vec2 particleOffset = vec2(
            hash12(particleCell + vec2(3.7, 8.1)),
            hash12(particleCell + vec2(9.2, 2.4))
        ) - 0.5;
        float particleDot = 1.0 - smoothstep(0.025, 0.12, length(particleLocal - particleOffset * 0.62));
        float particle = step(0.935, particleSeed) * particleDot;
        float darkSeed = hash12(particleCell + vec2(15.4, 7.8));
        float darkParticle = step(0.972, darkSeed) * particleDot;
        waterColor += vec3(0.68, 0.69, 0.62) * particle * (0.025 + intensity * 0.035);
        waterColor *= 1.0 - darkParticle * 0.055;
    }

    // As looking time increases the beautified input becomes more legible, while retaining the
    // water grade and a small amount of refraction so the result still reads as a pond reflection.
    vec3 readableFace = mix(waterColor, desaturatedReflection, settledFace * 0.42);
    readableFace = mix(readableFace, reflected, settledFace * 0.24);
    waterColor = mix(waterColor, readableFace, faceCore * (0.35 + reveal * 0.55));

    float vignette = smoothstep(0.20, 0.92, dot(p, p) * 1.25);
    waterColor *= 1.0 - vignette * (0.06 + uDarkness * 0.12);

    // Lake intensity controls the scene blend, not only distortion, so the setting remains easy to
    // understand in the workshop UI.
    vec3 finalColor = mix(base.rgb, waterColor, 0.38 + intensity * 0.62);
    fragColor = vec4(clamp(finalColor, 0.0, 1.0), base.a);
}
