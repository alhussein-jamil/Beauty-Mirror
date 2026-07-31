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

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash12(i);
    float b = hash12(i + vec2(1.0, 0.0));
    float c = hash12(i + vec2(0.0, 1.0));
    float d = hash12(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float amp = 0.55;
    mat2 m = mat2(0.80, 0.60, -0.60, 0.80);
    for (int i = 0; i < 4; i++) {
        v += amp * valueNoise(p);
        p = m * p * 2.03 + vec2(11.7, 3.1);
        amp *= 0.48;
    }
    return v;
}

float softEllipse(vec2 uv, vec2 center, vec2 radius) {
    vec2 d = (uv - center) / max(radius, vec2(0.001));
    return exp(-dot(d, d) * 1.55);
}

// Soft expanding crest — one mythic disturbance, not a radar stack.
float softRing(float dist, float travel, float width) {
    float d = abs(dist - travel);
    return exp(-(d * d) / max(width * width, 1e-5));
}

void main() {
    vec4 base = texture(uInput, vTexCoord);
    if (uEnabled < 0.5) {
        fragColor = base;
        return;
    }

    vec2 uv = vTexCoord;
    float aspect = uViewport.x / max(uViewport.y, 1.0);
    vec2 faceDelta = uv - uFaceCenter;
    faceDelta.x *= aspect;

    vec2 faceRadius = max(uFaceSize * vec2(0.78, 0.76), vec2(0.14, 0.18));
    float faceMask = softEllipse(uv, uFaceCenter, faceRadius) * uFacePresence;
    float faceCore = softEllipse(uv, uFaceCenter, faceRadius * vec2(0.72, 0.70)) * uFacePresence;
    float faceRim = clamp(faceMask - faceCore * 0.85, 0.0, 1.0);

    vec2 norm = faceDelta / max(vec2(faceRadius.x * aspect, faceRadius.y), vec2(0.001));
    float dist = length(norm);

    float reveal = smoothstep(0.02, 0.78, uVisitorReveal);
    float stillness = 1.0 - uMotion * 0.85;
    float portal = faceCore * mix(0.55, 0.96, uFaceClarity) * mix(0.35, 1.0, reveal);

    // Slow pond breath — low-frequency, almost glass when motion is low.
    float breathSpeed = mix(0.035, 0.14, uMotion);
    float t = uTime * breathSpeed;
    vec2 windUv = vec2(uv.x * aspect, uv.y) * mix(1.6, 3.2, uMotion);
    float breath = fbm(windUv + vec2(t * 0.55, -t * 0.31)) * 2.0 - 1.0;
    float breath2 = fbm(windUv * 1.7 + vec2(-t * 0.22, t * 0.41)) * 2.0 - 1.0;

    // Depth falls away from the face like looking into a dark well.
    float depth = smoothstep(0.15, 1.45, dist);
    float openWater = smoothstep(0.55, 1.35, dist);
    float shore = smoothstep(0.70, 1.55, dist);

    // Presence stirs the pond once into soft wavefronts; motion only keeps a hush of breath.
    float arrivalTravel = mix(0.20, 1.35, reveal);
    float arrivalRing = softRing(dist, arrivalTravel, 0.13) * reveal;
    float hushPhase = dist * 6.5 - t * 1.6;
    float hushWave = sin(hushPhase) * exp(-dist * 2.1) * uMotion * 0.28;
    float rings = (arrivalRing * 0.85 + hushWave) * uFacePresence;

    // Displacement stays gentle; portal stays nearly still so the face reads as a mirror.
    float waterAmp = mix(0.00035, 0.0028, uIntensity) * (0.20 + openWater * 0.80);
    waterAmp *= (1.0 - portal * 0.92) * (0.28 + uMotion * 0.72);
    vec2 drift = vec2(
        breath * 0.72 + breath2 * 0.28 + rings * 0.40,
        breath * 0.16 + breath2 * 0.58 + rings * 0.18
    );
    // Slight vertical stretch like a surface seen from above.
    drift.y *= 0.55;
    vec2 sampleUv = clamp(uv + drift * waterAmp * 18.0, vec2(0.0015), vec2(0.9985));

    vec3 reflected = texture(uInput, sampleUv).rgb;
    if (uQuality > 0.45) {
        // Tiny anisotropic smear — wet glass, not blur soup.
        vec2 smear = vec2(waterAmp * 2.4 + 0.0004, waterAmp * 0.9 + 0.0002);
        vec3 a = texture(uInput, clamp(sampleUv + smear, vec2(0.0015), vec2(0.9985))).rgb;
        vec3 b = texture(uInput, clamp(sampleUv - smear, vec2(0.0015), vec2(0.9985))).rgb;
        reflected = mix(reflected, (reflected * 2.0 + a + b) * 0.25, 0.16 + 0.10 * uIntensity * openWater);
    }

    float luma = dot(reflected, LUMA);
    // Mythic pond: ink, peat, cold silver depth — never pool-blue.
    vec3 abyss = vec3(0.035, 0.045, 0.050);
    vec3 peat = vec3(0.070, 0.085, 0.065);
    vec3 moss = vec3(0.095, 0.110, 0.080);
    vec3 silver = vec3(0.160, 0.170, 0.175);
    float depthGrad = smoothstep(0.05, 0.95, uv.y);
    vec3 waterBody = mix(abyss, peat, depthGrad * 0.55);
    waterBody = mix(waterBody, moss, openWater * 0.35);
    waterBody = mix(waterBody, silver, shore * 0.12);

    // Beer-like absorption: face stays luminous; water drinks the rest.
    float absorb = (0.34 + uDarkness * 0.52) * (0.25 + depth * 0.75);
    absorb *= (1.0 - portal * 0.88);
    vec3 toned = mix(reflected, waterBody + vec3(luma) * vec3(0.18, 0.22, 0.20), absorb);

    // Soft meniscus sheen around the face — Narcissus's mirror rim.
    float meniscus = faceRim * (0.35 + reveal * 0.65);
    float sheenBand = pow(max(0.0, 1.0 - abs(norm.y) * 1.8), 3.0) * faceMask;
    vec3 sheen = vec3(0.72, 0.74, 0.70) * (meniscus * 0.10 + sheenBand * 0.06) * (0.35 + uIntensity * 0.65);
    sheen *= (0.40 + stillness * 0.60);
    toned += sheen;

    // Barely-there film grain on deep water only.
    float grain = hash12(floor(uv * vec2(180.0, 120.0)) + floor(uTime * 3.0)) - 0.5;
    toned += grain * 0.012 * absorb;

    // Clear looking-glass face: mostly the beautified image, tiny wet glaze.
    vec3 mirrorFace = mix(base.rgb, reflected, 0.08 * (1.0 - uFaceClarity));
    mirrorFace = mix(mirrorFace, base.rgb * vec3(1.03, 1.025, 1.01) + vec3(0.01), 0.42 + 0.28 * uFaceClarity);
    // Cool night reflection grade on skin so it feels "in water", not a sticker.
    mirrorFace = mix(mirrorFace, mirrorFace * vec3(0.94, 0.97, 1.02), 0.18 * (1.0 - uFaceClarity * 0.5));
    vec3 color = mix(toned, mirrorFace, portal);

    // Soft halo where reflection meets water — readable, not neon.
    color += vec3(0.045, 0.040, 0.030) * faceRim * reveal * (0.08 + 0.10 * uIntensity);

    // Well walls: looking down into dark stone.
    vec2 well = uv - vec2(0.5);
    well.x *= aspect;
    float vignette = smoothstep(0.12, 0.98, dot(well, well) * 1.55);
    color *= 1.0 - vignette * (0.10 + 0.22 * uDarkness);

    // Outer frame sinks deeper as the visitor settles.
    color *= 1.0 - shore * (0.10 + uDarkness * 0.18) * (0.45 + reveal * 0.55);

    fragColor = vec4(clamp(color, 0.0, 1.0), base.a);
}
