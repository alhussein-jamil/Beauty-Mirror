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

    // Slightly tighter portal so a living water collar stays around the face.
    vec2 faceRadius = max(uFaceSize * vec2(0.72, 0.70), vec2(0.13, 0.17));
    float faceMask = softEllipse(uv, uFaceCenter, faceRadius * vec2(1.12, 1.10)) * uFacePresence;
    float faceCore = softEllipse(uv, uFaceCenter, faceRadius * vec2(0.78, 0.76)) * uFacePresence;
    float faceRim = clamp(faceMask - faceCore * 0.80, 0.0, 1.0);

    vec2 norm = faceDelta / max(vec2(faceRadius.x * aspect, faceRadius.y), vec2(0.001));
    float dist = length(norm);

    float reveal = smoothstep(0.02, 0.78, uVisitorReveal);
    // Motion floor: even low slider keeps pond alive; 0 = nearly glass, 1 = lively hush.
    float life = mix(0.42, 1.0, uMotion);
    float portal = faceCore * mix(0.48, 0.82, uFaceClarity) * mix(0.40, 1.0, reveal);

    float breathSpeed = mix(0.18, 0.55, uMotion);
    float t = uTime * breathSpeed;
    vec2 windUv = vec2(uv.x * aspect, uv.y) * mix(2.2, 3.8, uMotion);
    float breath = fbm(windUv + vec2(t * 0.85, -t * 0.48)) * 2.0 - 1.0;
    float breath2 = fbm(windUv * 1.65 + vec2(-t * 0.36, t * 0.62)) * 2.0 - 1.0;

    float depth = smoothstep(0.12, 1.40, dist);
    float openWater = smoothstep(0.35, 1.20, dist);
    float shore = smoothstep(0.65, 1.55, dist);

    // Continuous soft concentric hush — always moving, never a hard radar stack.
    float rippleSpeed = mix(0.70, 1.85, uMotion);
    float rippleA = sin(dist * 7.5 - uTime * rippleSpeed + breath * 0.35);
    float rippleB = sin(dist * 13.0 - uTime * rippleSpeed * 1.35 + 1.7) * 0.55;
    float hushWave = (rippleA * 0.65 + rippleB * 0.35) * exp(-dist * 1.15) * life;

    // Arrival crest rides visitor reveal, then blends into the continuous hush.
    float arrivalTravel = mix(0.15, 1.45, reveal);
    float arrivalRing = softRing(dist, arrivalTravel, 0.12) * reveal * 1.2;
    float rings = (hushWave + arrivalRing * 0.9) * mix(0.55, 1.0, uFacePresence);

    // UV bend strong enough to read on phone (~0.6–1.5% UV on open water).
    float waterAmp = mix(0.0045, 0.014, uIntensity);
    waterAmp *= mix(0.55, 1.0, openWater);
    // Wet-glass on the face: keep some motion so the mirror never freezes solid.
    waterAmp *= mix(0.28, 1.0, 1.0 - portal * 0.85);
    waterAmp *= life;

    vec2 drift = vec2(
        breath * 0.55 + breath2 * 0.25 + rings * 0.85,
        breath * 0.20 + breath2 * 0.55 + rings * 0.40
    );
    drift.y *= 0.62;
    vec2 sampleUv = clamp(uv + drift * waterAmp, vec2(0.0015), vec2(0.9985));

    vec3 reflected = texture(uInput, sampleUv).rgb;
    if (uQuality > 0.45) {
        vec2 smear = vec2(waterAmp * 0.55 + 0.0005, waterAmp * 0.22 + 0.00025);
        vec3 a = texture(uInput, clamp(sampleUv + smear, vec2(0.0015), vec2(0.9985))).rgb;
        vec3 b = texture(uInput, clamp(sampleUv - smear, vec2(0.0015), vec2(0.9985))).rgb;
        reflected = mix(reflected, (reflected * 2.0 + a + b) * 0.25, 0.18 + 0.12 * uIntensity * openWater);
    }

    float luma = dot(reflected, LUMA);
    vec3 abyss = vec3(0.035, 0.045, 0.050);
    vec3 peat = vec3(0.070, 0.085, 0.065);
    vec3 moss = vec3(0.095, 0.110, 0.080);
    vec3 silver = vec3(0.160, 0.170, 0.175);
    float depthGrad = smoothstep(0.05, 0.95, uv.y);
    vec3 waterBody = mix(abyss, peat, depthGrad * 0.55);
    waterBody = mix(waterBody, moss, openWater * 0.35);
    waterBody = mix(waterBody, silver, shore * 0.12);

    float absorb = (0.34 + uDarkness * 0.52) * (0.25 + depth * 0.75);
    absorb *= (1.0 - portal * 0.88);
    vec3 toned = mix(reflected, waterBody + vec3(luma) * vec3(0.18, 0.22, 0.20), absorb);

    // Color-domain life: drifting caustics + traveling soft crest light (reads even if UV is subtle).
    float caustic = fbm(windUv * 2.4 + vec2(uTime * 0.22, -uTime * 0.16));
    float shimmer = pow(clamp(caustic, 0.0, 1.0), 2.6) * (0.035 + 0.070 * uIntensity) * life;
    shimmer *= mix(0.35, 1.0, openWater + faceRim * 0.65);
    toned += vec3(0.62, 0.66, 0.58) * shimmer;

    float crestTravel = fract(uTime * mix(0.10, 0.26, uMotion));
    float crestLight = softRing(dist, crestTravel * 1.55, 0.09) * exp(-dist * 0.85);
    toned += vec3(0.48, 0.50, 0.45) * crestLight * (0.045 + 0.06 * uIntensity) * life;

    float meniscus = faceRim * (0.40 + reveal * 0.60);
    float sheenBand = pow(max(0.0, 1.0 - abs(norm.y) * 1.8), 3.0) * faceMask;
    // Breathing rim highlight — face edge proves the surface moves.
    float rimPulse = 0.55 + 0.45 * sin(uTime * mix(0.9, 2.0, uMotion) + breath * 1.2);
    vec3 sheen = vec3(0.72, 0.74, 0.70) * (meniscus * 0.12 + sheenBand * 0.07) * (0.40 + uIntensity * 0.60);
    sheen *= rimPulse * life;
    toned += sheen;

    float grain = hash12(floor(uv * vec2(180.0, 120.0)) + floor(uTime * 6.0)) - 0.5;
    toned += grain * 0.014 * absorb * life;

    // Readable face with wet-glass refraction — never a frozen sticker.
    float wetMix = mix(0.22, 0.08, uFaceClarity);
    vec3 mirrorFace = mix(base.rgb, reflected, wetMix);
    mirrorFace = mix(mirrorFace, base.rgb * vec3(1.03, 1.025, 1.01) + vec3(0.01), 0.38 + 0.30 * uFaceClarity);
    mirrorFace = mix(mirrorFace, mirrorFace * vec3(0.94, 0.97, 1.02), 0.16 * (1.0 - uFaceClarity * 0.5));
    // Soft rim of live water into the face edge.
    mirrorFace = mix(mirrorFace, toned, faceRim * 0.18 * life);
    vec3 color = mix(toned, mirrorFace, portal);

    color += vec3(0.045, 0.040, 0.030) * faceRim * reveal * (0.08 + 0.10 * uIntensity);

    vec2 well = uv - vec2(0.5);
    well.x *= aspect;
    float vignette = smoothstep(0.12, 0.98, dot(well, well) * 1.55);
    color *= 1.0 - vignette * (0.10 + 0.22 * uDarkness);
    color *= 1.0 - shore * (0.10 + uDarkness * 0.18) * (0.45 + reveal * 0.55);

    fragColor = vec4(clamp(color, 0.0, 1.0), base.a);
}
