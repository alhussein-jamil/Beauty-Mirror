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
uniform float uCameraBlend;
uniform float uDeformation;
uniform float uSwirl;
uniform float uQuality;
uniform float uEnabled;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);
const float PI = 3.141592653589793;
// vTexCoord.y = 1 at top of the phone screen — keep the sun high in frame.
const vec2 SUN_UV = vec2(0.58, 0.86);

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float softEllipse(vec2 uv, vec2 center, vec2 radius) {
    vec2 d = (uv - center) / max(radius, vec2(0.001));
    return exp(-dot(d, d) * 1.36);
}

mat2 rotate2d(float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat2(c, -s, s, c);
}

void addDirectionalWave(
    inout vec3 field,
    vec2 p,
    vec2 direction,
    float frequency,
    float speed,
    float amplitude,
    float phase,
    float time
) {
    vec2 dir = normalize(direction);
    float angle = dot(p, dir) * frequency + time * speed + phase;
    float wave = sin(angle) * amplitude;
    float slope = cos(angle) * amplitude * frequency;
    field.x += wave;
    field.yz += dir * slope;
}

void addRadialWave(
    inout vec3 field,
    vec2 p,
    vec2 center,
    float frequency,
    float speed,
    float amplitude,
    float phase,
    float time
) {
    vec2 delta = p - center;
    float radius = max(length(delta), 0.001);
    float angle = radius * frequency - time * speed + phase;
    float envelope = exp(-radius * 0.55);
    float wave = sin(angle) * amplitude * envelope;
    float slope = cos(angle) * amplitude * frequency * envelope;
    field.x += wave;
    field.yz += (delta / radius) * slope;
}

vec3 waterField(vec2 p, float time, float motion, float quality) {
    vec3 field = vec3(0.0);
    float speed = 0.52 + motion * 0.95;
    float scale = 1.15 + motion * 0.80;

    // Readable wind swells — sharp enough to show in a still frame.
    addDirectionalWave(field, p, vec2(1.0, 0.10), 8.5, speed * 0.70, 0.028 * scale, 0.15, time);
    addDirectionalWave(field, p, vec2(-0.18, 1.0), 12.0, speed * 0.52, 0.020 * scale, 1.4, time);
    addDirectionalWave(field, p, vec2(0.78, 0.48), 19.0, speed * 1.05, 0.011 * scale, 3.8, time);
    addDirectionalWave(field, p, vec2(0.08, -1.0), 6.0, speed * 0.38, 0.022 * scale, 2.6, time);

    vec2 driftA = vec2(sin(time * 0.11) * 0.40, cos(time * 0.08) * 0.26);
    vec2 driftB = vec2(cos(time * 0.13) * 0.32, sin(time * 0.09) * 0.38);
    addRadialWave(field, p, driftA, 16.0, speed * 0.48, 0.0090 * scale, 0.3, time);
    addRadialWave(field, p, driftB, 21.0, speed * 0.62, 0.0065 * scale, 1.7, time);

    if (quality > 0.26) {
        addDirectionalWave(field, p, vec2(-0.88, 0.38), 28.0, speed * 1.12, 0.0050 * scale, 2.1, time);
        addRadialWave(field, p, vec2(-0.30, -0.18) + driftA * 0.25, 26.0, speed * 0.78, 0.0048 * scale, 0.9, time);
    }
    if (quality > 0.58) {
        addDirectionalWave(field, p, vec2(0.35, 0.92), 40.0, speed * 1.28, 0.0028 * scale, 4.6, time);
        addRadialWave(field, p, vec2(0.28, 0.20) + driftB * 0.20, 34.0, speed * 0.95, 0.0032 * scale, 3.0, time);
    }
    return field;
}

vec3 skyColor(vec2 reflectedUv, float time) {
    float h = clamp(reflectedUv.y, 0.0, 1.0);
    vec3 zenith = vec3(0.16, 0.46, 0.88);
    vec3 mid = vec3(0.42, 0.72, 0.96);
    vec3 haze = vec3(0.78, 0.88, 0.96);
    vec3 sky = mix(haze, mid, smoothstep(0.0, 0.42, h));
    sky = mix(sky, zenith, smoothstep(0.42, 1.0, h));

    float cloudA = sin((reflectedUv.x * 2.6 + reflectedUv.y * 0.5) * PI + time * 0.045);
    float cloudB = sin((reflectedUv.x * -1.6 + reflectedUv.y * 1.9) * PI - time * 0.03);
    float cloud = smoothstep(0.30, 1.1, cloudA * 0.52 + cloudB * 0.40 + 0.48);
    sky = mix(sky, vec3(0.93, 0.96, 1.0), cloud * 0.16 * smoothstep(0.2, 0.9, h));

    // Compact sun disk near the top — minimal shaft so it reads as the sun, not a flare.
    vec2 sunDelta = reflectedUv - SUN_UV;
    sunDelta.x *= 1.35;
    float sunDist = length(sunDelta);
    float sunCore = smoothstep(0.040, 0.0, sunDist);
    float sunHalo = exp(-sunDist * 14.0) * 0.85 + exp(-sunDist * 5.5) * 0.28;
    sky += vec3(1.0, 0.96, 0.82) * sunCore * 1.8;
    sky += vec3(1.0, 0.84, 0.50) * sunHalo * 0.75;
    return sky;
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
    float depth = clamp(uDarkness, 0.0, 1.0);
    float reveal = clamp(uVisitorReveal, 0.0, 1.0);
    float presence = clamp(uFacePresence, 0.0, 1.0);
    float quality = clamp(uQuality, 0.0, 1.0);
    float deformation = clamp(uDeformation, 0.0, 1.0);
    float swirlStrength = clamp(uSwirl, 0.0, 1.0);

    vec3 field = waterField(p, time, motion, quality);
    vec3 normal = normalize(vec3(-field.y * 0.085, -field.z * 0.085, 1.0));

    // Sun from above / slightly to the side.
    vec3 sunDir = normalize(vec3(0.22, 0.78, 0.58));
    vec3 fillDir = normalize(vec3(-0.55, 0.30, 0.72));
    float sunSpec = pow(max(dot(normal, sunDir), 0.0), mix(22.0, 64.0, quality));
    float fillSpec = pow(max(dot(normal, fillDir), 0.0), mix(12.0, 28.0, quality));
    float fresnel = pow(1.0 - clamp(normal.z, 0.0, 1.0), 2.2);

    vec2 reflectOffset = vec2(normal.x, -normal.y) * (0.12 + motion * 0.07);
    reflectOffset.x /= max(aspect, 0.001);
    vec2 skyUv = clamp(uv + reflectOffset, vec2(0.0), vec2(1.0));
    skyUv.y = clamp(skyUv.y * 0.72 + 0.28, 0.0, 1.0);
    vec3 sky = skyColor(skyUv, time);

    vec3 deepWater = vec3(0.03, 0.20, 0.42);
    vec3 midWater = vec3(0.08, 0.38, 0.62);
    vec3 water = mix(deepWater, midWater, 0.38 + fresnel * 0.28);
    water = mix(water, sky, 0.58 - depth * 0.18 + fresnel * 0.20);

    float crest = smoothstep(0.004, 0.028, abs(field.x));
    float crestLine = smoothstep(0.018, 0.004, abs(field.x));
    water += vec3(0.70, 0.90, 1.0) * crest * (0.06 + motion * 0.07);
    water += vec3(0.95, 0.98, 1.0) * crestLine * (0.08 + motion * 0.06);
    water += vec3(1.0, 0.95, 0.78) * sunSpec * (0.55 + intensity * 0.35 + motion * 0.12);
    water += vec3(0.78, 0.92, 1.0) * fillSpec * (0.12 + intensity * 0.06);

    float glitterSeed = hash12(floor(uv * vec2(55.0, 80.0) + field.yz * 22.0 + time * 0.45));
    float glitter = step(0.982 - motion * 0.010, glitterSeed) * smoothstep(0.25, 0.85, sunSpec);
    water += vec3(1.0, 0.98, 0.90) * glitter * (0.35 + quality * 0.15);

    if (quality > 0.20) {
        float caustic = sin((p.x * 11.0 + field.y * 18.0) + time * 0.62) *
            sin((p.y * 8.5 - field.z * 15.0) - time * 0.46);
        caustic = pow(smoothstep(0.45, 0.95, caustic * 0.5 + 0.5), 1.4);
        water += vec3(0.70, 0.88, 1.0) * caustic * (0.055 + motion * 0.045) * (0.50 + sunSpec);
    }

    // Persistent highlights reapplied after camera merge.
    vec3 surfaceAccent = vec3(1.0, 0.95, 0.78) * sunSpec * (0.28 + motion * 0.14);
    surfaceAccent += vec3(0.70, 0.90, 1.0) * crest * (0.07 + motion * 0.07);
    surfaceAccent += vec3(0.95, 0.98, 1.0) * crestLine * 0.06;
    surfaceAccent += vec3(1.0, 0.98, 0.90) * glitter * 0.35;

    vec3 finalColor = water;

    if (presence > 0.01) {
        vec2 faceP = uFaceCenter - vec2(0.5);
        faceP.x *= aspect;
        vec2 faceDelta = p - faceP;
        float faceRadius = max(max(uFaceSize.x * aspect, uFaceSize.y) * 0.92, 0.18);
        float radius = length(faceDelta);
        float normalizedRadius = radius / max(faceRadius, 0.001);
        float vortexFalloff = 1.0 - smoothstep(0.08, 1.25, normalizedRadius);

        // Arrival swirl only — continuous waves remain from waterField forever.
        float swirlEnvelope = presence * smoothstep(0.005, 0.05, reveal) *
            (1.0 - smoothstep(0.22, 0.50, reveal));
        float vortexAngle = swirlEnvelope * swirlStrength * vortexFalloff * vortexFalloff * 1.15;
        vec2 swirledDelta = rotate2d(vortexAngle) * faceDelta;
        vec2 swirledP = faceP + swirledDelta;

        float arrivalPhase = reveal * 2.0;
        float arrivalRingRadius = 0.05 + arrivalPhase * faceRadius * 1.45;
        float ringDistance = abs(radius - arrivalRingRadius);
        float arrivalRing = exp(-ringDistance * ringDistance / max(0.00028, faceRadius * faceRadius * 0.005));
        arrivalRing *= presence * (1.0 - smoothstep(0.50, 0.92, reveal));
        water += vec3(0.80, 0.95, 1.0) * arrivalRing * (0.10 + swirlStrength * 0.08);

        float polarAngle = atan(faceDelta.y, faceDelta.x);
        float spiralWave = sin(radius * 36.0 - polarAngle * 2.8 - time * (1.6 + motion * 1.8)) *
            vortexFalloff * swirlEnvelope;
        water += vec3(0.72, 0.92, 1.0) * max(spiralWave, 0.0) * (0.055 + swirlStrength * 0.055);

        // Ongoing wave refraction over the visitor for the whole look — not only arrival.
        vec2 normalOffset = vec2(field.y, field.z) * (0.0018 + deformation * 0.012 + motion * 0.0035);
        normalOffset.x /= max(aspect, 0.001);
        vec2 swirlUv = swirledP;
        swirlUv.x /= max(aspect, 0.001);
        swirlUv += vec2(0.5);
        vec2 cameraUv = mix(uv, swirlUv, swirlEnvelope * swirlStrength * 0.70);
        cameraUv += normalOffset * (0.90 + deformation * 1.40);
        cameraUv += normalize(faceDelta + vec2(0.0001)) * arrivalRing * deformation * 0.0028;
        cameraUv = clamp(cameraUv, vec2(0.002), vec2(0.998));

        vec3 camera = texture(uInput, cameraUv).rgb;
        float cameraLuma = dot(camera, LUMA);
        camera = mix(vec3(cameraLuma), camera, 0.92 + reveal * 0.08);
        camera = mix(camera, camera * vec3(0.94, 1.01, 1.06), 0.12 + intensity * 0.06);

        vec2 faceRadiusUv = max(uFaceSize * vec2(1.15, 1.22), vec2(0.22, 0.28));
        float broadFace = softEllipse(uv, uFaceCenter, faceRadiusUv) * presence;
        float faceGuide = clamp(broadFace * 0.85, 0.0, 1.0);

        float revealBlend = 0.32 + smoothstep(0.0, 0.70, reveal) * 0.68;
        float globalCamera = presence * uCameraBlend * revealBlend * (0.60 + intensity * 0.14);
        float faceCamera = presence * uFaceClarity * (0.62 + reveal * 0.38);
        float cameraMix = clamp(mix(globalCamera, max(globalCamera, faceCamera), faceGuide), 0.0, 1.0);

        // Keep pond alive after settle, but never bury the face in blue.
        // Outside the face: more water. On the face: thin living veil + specular only.
        float waterRemain = mix(0.28 + motion * 0.08 + fresnel * 0.12, 0.10 + motion * 0.05, faceGuide);
        waterRemain = clamp(waterRemain, 0.08, 0.40);
        cameraMix = min(cameraMix, 1.0 - waterRemain);

        float protectedMix = faceGuide * clamp(uFaceClarity, 0.0, 1.0);
        vec3 reflection = mix(camera, texture(uInput, uv).rgb, protectedMix * deformation * 0.28);
        finalColor = mix(water, reflection, cameraMix);

        // Living veil: stronger off-face, light on-face so motion stays without erasing features.
        float livingVeil = mix(0.18 + deformation * 0.10 + motion * 0.08, 0.04 + motion * 0.03, faceGuide);
        livingVeil *= presence;
        finalColor = mix(finalColor, water, livingVeil);
        finalColor += surfaceAccent * mix(0.90, 0.35, faceGuide);
        finalColor += vec3(0.85, 0.95, 1.0) * fresnel * mix(0.10, 0.03, faceGuide) * presence;
    } else {
        finalColor += surfaceAccent * 0.40;
    }

    float vignette = smoothstep(0.28, 0.95, dot(p, p) * 1.15);
    finalColor *= 1.0 - vignette * (0.028 + depth * 0.040);
    finalColor = mix(finalColor, finalColor * vec3(0.93, 0.98, 1.05), depth * 0.12);
    float sunWarm = smoothstep(0.50, 0.0, length((uv - SUN_UV) * vec2(1.3, 1.1)));
    finalColor = mix(finalColor, finalColor * vec3(1.06, 1.02, 0.96), sunWarm * 0.14);
    finalColor = mix(vec3(dot(finalColor, LUMA)), finalColor, 0.90 + intensity * 0.10);

    fragColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
}
