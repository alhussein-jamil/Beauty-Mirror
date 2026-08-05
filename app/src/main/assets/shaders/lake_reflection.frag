#version 300 es
precision highp float;
in vec2 vTexCoord;
uniform sampler2D uInput;
uniform sampler2D uFaceMask;
uniform float uHasFaceMask;
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

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash12(i);
    float b = hash12(i + vec2(1.0, 0.0));
    float c = hash12(i + vec2(0.0, 1.0));
    float d = hash12(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float pondNoise(vec2 p, float time, float quality) {
    float n = valueNoise(p);
    n += valueNoise(p * 2.03 + vec2(7.3, -3.1) + time * 0.007) * 0.48;
    if (quality > 0.44) {
        n += valueNoise(p * 4.11 + vec2(-5.6, 9.8) - time * 0.004) * 0.22;
    }
    return n / (quality > 0.44 ? 1.70 : 1.48);
}

float ring(float radius, float centerRadius, float width) {
    float d = (radius - centerRadius) / max(width, 0.001);
    return exp(-d * d);
}

float softEllipse(vec2 uv, vec2 center, vec2 radius) {
    vec2 d = (uv - center) / max(radius, vec2(0.001));
    return exp(-dot(d, d) * 1.72);
}

float softBand(float value, float center, float halfWidth) {
    return 1.0 - smoothstep(halfWidth, halfWidth * 2.5, abs(value - center));
}

void main() {
    if (uEnabled < 0.5) {
        fragColor = texture(uInput, vTexCoord);
        return;
    }

    vec2 uv = vTexCoord;
    float aspect = uViewport.x / max(uViewport.y, 1.0);
    vec2 p = uv - vec2(0.5);
    p.x *= aspect;

    float time = uTime;
    float motion = clamp(uMotion, 0.0, 1.0);
    float intensity = clamp(uIntensity, 0.0, 1.0);
    float darkness = clamp(uDarkness, 0.0, 1.0);
    float reveal = clamp(uVisitorReveal, 0.0, 1.0);
    float presence = clamp(uFacePresence, 0.0, 1.0);
    float quality = clamp(uQuality, 0.0, 1.0);

    // Slow low-frequency movement: murky pond, never a turquoise swimming-pool caustic pattern.
    vec2 drift = vec2(time * (0.010 + motion * 0.015), -time * (0.006 + motion * 0.010));
    float murkA = pondNoise(p * 1.45 + drift, time, quality);
    float murkB = pondNoise(p * 2.35 - drift * 0.55 + vec2(3.8, -1.2), time, quality);
    float broadMurk = clamp(murkA * 0.68 + murkB * 0.32, 0.0, 1.0);

    vec3 deepPeat = vec3(0.075, 0.087, 0.066);
    vec3 siltGrey = vec3(0.285, 0.302, 0.285);
    vec3 paleSky = vec3(0.455, 0.470, 0.445);
    vec3 pond = mix(deepPeat, siltGrey, 0.30 + broadMurk * 0.58);

    // Broad pale workshop-light / sky reflections. Their edges drift slowly and remain blurred.
    float warpedY = uv.y + sin(uv.x * 4.3 + time * 0.020) * 0.012 + (murkB - 0.5) * 0.020;
    float skyBandA = softBand(warpedY, 0.205 + sin(time * 0.011) * 0.018, 0.020);
    float skyBandB = softBand(warpedY, 0.745 + sin(time * 0.009 + 2.1) * 0.020, 0.026);
    float skyWash = smoothstep(0.62, 0.98, uv.x + murkA * 0.18) * 0.16;
    pond = mix(pond, paleSky, skyBandA * 0.15 + skyBandB * 0.11 + skyWash);

    // Dark bank/wood/reflection silhouettes around the perimeter, inspired by the supplied pond.
    float leftBankNoise = valueNoise(vec2(uv.y * 5.2 + time * 0.006, 4.7));
    float topBankNoise = valueNoise(vec2(uv.x * 5.8 - time * 0.004, 8.2));
    float leftBank = 1.0 - smoothstep(0.035, 0.16, uv.x + (leftBankNoise - 0.5) * 0.11);
    float topBank = 1.0 - smoothstep(0.025, 0.14, uv.y + (topBankNoise - 0.5) * 0.085);
    pond *= 1.0 - (leftBank * 0.55 + topBank * 0.34) * (0.72 + darkness * 0.22);

    // Three independent expanding ripples keep the no-face state alive as an animated screensaver.
    float rippleValue = 0.0;
    vec2 rippleSlope = vec2(0.0);
    vec2 centers[3];
    centers[0] = vec2(0.24, 0.35);
    centers[1] = vec2(0.77, 0.67);
    centers[2] = vec2(0.56, 0.18);
    for (int i = 0; i < 3; ++i) {
        if (i == 2 && quality < 0.46) break;
        vec2 delta = uv - centers[i];
        delta.x *= aspect;
        float radius = length(delta);
        float cycle = fract(time * (0.020 + float(i) * 0.004 + motion * 0.012) + float(i) * 0.31);
        float centerRadius = 0.035 + cycle * (0.43 + float(i) * 0.045);
        float width = 0.014 + cycle * 0.014 + motion * 0.006;
        float crest = ring(radius, centerRadius, width);
        float trough = ring(radius, max(0.0, centerRadius - 0.036), width * 0.82);
        float wave = (crest - trough * 0.52) * (1.0 - cycle) * (0.55 + motion * 0.45);
        rippleValue += wave;
        rippleSlope += delta / max(radius, 0.001) * wave;
    }

    // One visitor-arrival wave spreads from the detected face during the reveal.
    vec2 faceDelta = uv - uFaceCenter;
    faceDelta.x *= aspect;
    float faceDistance = length(faceDelta);
    float arrivalRadius = 0.025 + reveal * 0.72;
    float arrivalEnvelope = smoothstep(0.01, 0.09, reveal) * (1.0 - smoothstep(0.74, 1.0, reveal));
    float arrival = ring(faceDistance, arrivalRadius, 0.020 + motion * 0.012) -
        ring(faceDistance, max(0.0, arrivalRadius - 0.045), 0.017 + motion * 0.009) * 0.48;
    arrival *= arrivalEnvelope * presence;
    rippleValue += arrival * 0.72;
    rippleSlope += faceDelta / max(faceDistance, 0.001) * arrival * 1.8;

    float rippleLight = max(rippleValue, 0.0);
    float rippleDark = max(-rippleValue, 0.0);
    pond += paleSky * rippleLight * (0.055 + intensity * 0.035);
    pond *= 1.0 - rippleDark * 0.035;

    // Stable suspended mineral/silt particles. They drift imperceptibly instead of sparkling.
    if (quality > 0.22) {
        vec2 particleUv = uv + vec2(time * 0.0007, -time * 0.00035);
        vec2 grid = particleUv * vec2(74.0, 126.0);
        vec2 cell = floor(grid);
        vec2 local = fract(grid) - 0.5;
        float seed = hash12(cell);
        vec2 offset = vec2(hash12(cell + vec2(3.1)), hash12(cell + vec2(8.7))) - 0.5;
        float dotShape = 1.0 - smoothstep(0.025, 0.115, length(local - offset * 0.60));
        float paleParticle = step(0.943, seed) * dotShape;
        float darkParticle = step(0.978, hash12(cell + vec2(14.2))) * dotShape;
        pond += vec3(0.64, 0.65, 0.59) * paleParticle * 0.035;
        pond *= 1.0 - darkParticle * 0.07;
    }

    // The idle state is now purely the animated pond. The camera is sampled only near a face.
    vec3 finalColor = pond;
    vec2 faceRadius = max(uFaceSize * vec2(0.60, 0.62), vec2(0.10, 0.14));
    float ellipseGate = softEllipse(uv, uFaceCenter, faceRadius) * presence;
    if (ellipseGate > 0.002 && presence > 0.01) {
        float maskSample = uHasFaceMask > 0.5 ? texture(uFaceMask, uv).r : ellipseGate;
        // Actual landmark mask is authoritative. A tiny ellipse halo only softens the cutout edge.
        float faceShape = clamp(max(maskSample, ellipseGate * 0.075), 0.0, 1.0) * presence;

        // Minimal surface movement on the face: enough to read as reflection, never enough to hide it.
        float earlyWater = mix(1.0, 0.20, smoothstep(0.0, 0.82, reveal));
        vec2 faceDistortion = rippleSlope * (0.00030 + motion * 0.00048) * earlyWater;
        faceDistortion.x /= max(aspect, 0.001);
        faceDistortion = clamp(faceDistortion, vec2(-0.0022), vec2(0.0022));
        vec2 faceUv = clamp(uv + faceDistortion, vec2(0.002), vec2(0.998));
        vec3 reflection = texture(uInput, faceUv).rgb;

        if (quality > 0.62 && reveal < 0.72) {
            vec2 softStep = normalize(rippleSlope + vec2(0.001)) * 0.00055 * earlyWater;
            softStep.x /= max(aspect, 0.001);
            vec3 nearby = texture(uInput, clamp(faceUv + softStep, vec2(0.002), vec2(0.998))).rgb;
            reflection = mix(reflection, nearby, 0.12 * (1.0 - reveal));
        }

        float reflectionLuma = dot(reflection, LUMA);
        float colorReturn = 0.72 + reveal * 0.28;
        reflection = mix(vec3(reflectionLuma), reflection, colorReturn);

        // Water tint over the face falls from 11% to 2.5%; this explicitly addresses the client note.
        float veil = mix(0.11, 0.025, smoothstep(0.0, 0.78, reveal));
        reflection = mix(reflection, pond, veil * (0.65 + motion * 0.35));
        reflection += paleSky * max(arrival, 0.0) * 0.018 * (1.0 - reveal);

        // The mirror is legible immediately; looking longer reveals full clarity and accumulated beauty.
        float faceAlpha = faceShape * clamp(uFaceClarity, 0.0, 1.0) *
            (0.76 + smoothstep(0.0, 0.70, reveal) * 0.24);
        finalColor = mix(pond, reflection, clamp(faceAlpha, 0.0, 1.0));
    }

    float vignette = smoothstep(0.22, 0.94, dot(p, p) * 1.30);
    finalColor *= 1.0 - vignette * (0.045 + darkness * 0.085);
    finalColor = mix(vec3(dot(finalColor, LUMA)), finalColor, 0.70 + intensity * 0.24);
    finalColor *= mix(1.02, 0.84, darkness * 0.60);

    fragColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
}
