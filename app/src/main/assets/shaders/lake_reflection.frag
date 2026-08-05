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
uniform float uSettledWater;
uniform float uSettledCamera;
uniform float uRippleRegions;
uniform float uRippleSpeed;
uniform float uWaveDetail;
uniform float uSpecular;
uniform float uSkyBlue;
uniform float uQuality;
uniform float uEnabled;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);
const float PI = 3.141592653589793;

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

vec3 waterField(
    vec2 p,
    float time,
    float motion,
    float quality,
    float rippleRegions,
    float rippleSpeed,
    float waveDetail
) {
    vec3 field = vec3(0.0);
    float speed = (0.28 + motion * 0.85) * (0.35 + rippleSpeed * 1.35);
    float scale = 0.70 + motion * 0.95;
    float detail = clamp(waveDetail, 0.0, 1.0);
    float regions = clamp(rippleRegions, 0.0, 1.0);

    // Base wind chop — amount scales with wave detail (not fixed “3 regions”).
    addDirectionalWave(field, p, vec2(1.0, 0.10), 7.5, speed * 0.70, 0.018 * scale * (0.35 + detail), 0.15, time);
    if (detail > 0.18) {
        addDirectionalWave(field, p, vec2(-0.18, 1.0), 11.0, speed * 0.52, 0.014 * scale * detail, 1.4, time);
    }
    if (detail > 0.40) {
        addDirectionalWave(field, p, vec2(0.78, 0.48), 17.0, speed * 1.05, 0.009 * scale * detail, 3.8, time);
    }
    if (detail > 0.62) {
        addDirectionalWave(field, p, vec2(0.08, -1.0), 5.5, speed * 0.38, 0.016 * scale * detail, 2.6, time);
    }
    if (detail > 0.82 && quality > 0.26) {
        addDirectionalWave(field, p, vec2(-0.88, 0.38), 26.0, speed * 1.12, 0.0045 * scale * detail, 2.1, time);
    }

    // Controllable count of independent ripple regions (0 → none, 1 → six).
    vec2 c0 = vec2(sin(time * 0.11) * 0.40, cos(time * 0.08) * 0.26);
    vec2 c1 = vec2(cos(time * 0.13) * 0.32, sin(time * 0.09) * 0.38);
    vec2 c2 = vec2(-0.30, -0.18) + c0 * 0.25;
    vec2 c3 = vec2(0.28, 0.20) + c1 * 0.20;
    vec2 c4 = vec2(0.05, 0.42) + vec2(sin(time * 0.07), cos(time * 0.12)) * 0.18;
    vec2 c5 = vec2(-0.38, 0.30) + vec2(cos(time * 0.09), sin(time * 0.06)) * 0.16;

    if (regions > 0.06) {
        addRadialWave(field, p, c0, 15.0, speed * 0.48, 0.0085 * scale, 0.3, time);
    }
    if (regions > 0.20) {
        addRadialWave(field, p, c1, 19.0, speed * 0.62, 0.0068 * scale, 1.7, time);
    }
    if (regions > 0.36 && quality > 0.20) {
        addRadialWave(field, p, c2, 24.0, speed * 0.78, 0.0052 * scale, 0.9, time);
    }
    if (regions > 0.52 && quality > 0.26) {
        addRadialWave(field, p, c3, 30.0, speed * 0.95, 0.0040 * scale, 3.0, time);
    }
    if (regions > 0.68 && quality > 0.40) {
        addRadialWave(field, p, c4, 22.0, speed * 0.70, 0.0046 * scale, 2.2, time);
    }
    if (regions > 0.84 && quality > 0.55) {
        addRadialWave(field, p, c5, 34.0, speed * 1.05, 0.0034 * scale, 4.1, time);
    }
    return field;
}

vec3 skyColor(vec2 reflectedUv, float time, float skyBlue) {
    float h = clamp(reflectedUv.y, 0.0, 1.0);
    float lift = clamp(skyBlue, 0.0, 1.0);
    // True sky-blue range — light cyan-blue, not deep navy.
    vec3 zenith = mix(vec3(0.28, 0.58, 0.92), vec3(0.45, 0.74, 0.98), lift);
    vec3 mid = mix(vec3(0.52, 0.78, 0.96), vec3(0.68, 0.88, 1.0), lift);
    vec3 haze = mix(vec3(0.78, 0.90, 0.98), vec3(0.88, 0.95, 1.0), lift);
    vec3 sky = mix(haze, mid, smoothstep(0.0, 0.45, h));
    sky = mix(sky, zenith, smoothstep(0.45, 1.0, h));

    float cloudA = sin((reflectedUv.x * 2.4 + reflectedUv.y * 0.5) * PI + time * 0.04);
    float cloudB = sin((reflectedUv.x * -1.5 + reflectedUv.y * 1.8) * PI - time * 0.028);
    float cloud = smoothstep(0.28, 1.05, cloudA * 0.50 + cloudB * 0.38 + 0.48);
    sky = mix(sky, vec3(0.95, 0.98, 1.0), cloud * (0.10 + lift * 0.10) * smoothstep(0.15, 0.9, h));
    // No sun disk / shaft — only soft sky + clouds.
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
    float settledWater = clamp(uSettledWater, 0.0, 1.0);
    float settledCamera = clamp(uSettledCamera, 0.0, 1.0);
    float specularAmt = clamp(uSpecular, 0.0, 1.0);
    float skyBlue = clamp(uSkyBlue, 0.0, 1.0);

    vec3 field = waterField(
        p, time, motion, quality,
        clamp(uRippleRegions, 0.0, 1.0),
        clamp(uRippleSpeed, 0.0, 1.0),
        clamp(uWaveDetail, 0.0, 1.0)
    );
    vec3 normal = normalize(vec3(-field.y * 0.075, -field.z * 0.075, 1.0));

    vec3 lightDir = normalize(vec3(0.20, 0.55, 0.75));
    vec3 fillDir = normalize(vec3(-0.50, 0.25, 0.72));
    float sunSpec = pow(max(dot(normal, lightDir), 0.0), mix(20.0, 58.0, quality)) * specularAmt;
    float fillSpec = pow(max(dot(normal, fillDir), 0.0), mix(12.0, 26.0, quality)) * specularAmt * 0.65;
    float fresnel = pow(1.0 - clamp(normal.z, 0.0, 1.0), 2.2);

    vec2 reflectOffset = vec2(normal.x, -normal.y) * (0.10 + motion * 0.06);
    reflectOffset.x /= max(aspect, 0.001);
    vec2 skyUv = clamp(uv + reflectOffset, vec2(0.0), vec2(1.0));
    skyUv.y = clamp(skyUv.y * 0.75 + 0.25, 0.0, 1.0);
    vec3 sky = skyColor(skyUv, time, skyBlue);

    vec3 deepWater = mix(vec3(0.10, 0.38, 0.62), vec3(0.22, 0.58, 0.82), skyBlue);
    vec3 midWater = mix(vec3(0.28, 0.62, 0.86), vec3(0.48, 0.78, 0.96), skyBlue);
    vec3 water = mix(deepWater, midWater, 0.40 + fresnel * 0.28 - depth * 0.20);
    water = mix(water, sky, 0.55 + skyBlue * 0.18 + fresnel * 0.16 - depth * 0.15);

    float crest = smoothstep(0.004, 0.028, abs(field.x));
    float crestLine = smoothstep(0.018, 0.004, abs(field.x));
    water += vec3(0.78, 0.92, 1.0) * crest * (0.045 + motion * 0.055);
    water += vec3(0.95, 0.98, 1.0) * crestLine * (0.05 + motion * 0.04) * specularAmt;
    water += vec3(0.95, 0.97, 1.0) * sunSpec * (0.35 + intensity * 0.30 + motion * 0.08);
    water += vec3(0.82, 0.93, 1.0) * fillSpec * (0.08 + intensity * 0.05);

    float glitterSeed = hash12(floor(uv * vec2(55.0, 80.0) + field.yz * 22.0 + time * 0.45));
    float glitter = step(0.984 - motion * 0.008, glitterSeed) * smoothstep(0.20, 0.80, sunSpec);
    water += vec3(1.0, 0.99, 0.96) * glitter * (0.22 + specularAmt * 0.20);

    if (quality > 0.20 && motion > 0.05) {
        float caustic = sin((p.x * 10.0 + field.y * 16.0) + time * 0.55) *
            sin((p.y * 8.0 - field.z * 14.0) - time * 0.42);
        caustic = pow(smoothstep(0.45, 0.95, caustic * 0.5 + 0.5), 1.35);
        water += vec3(0.75, 0.90, 1.0) * caustic * (0.035 + motion * 0.035) * specularAmt;
    }

    vec3 surfaceAccent = vec3(0.95, 0.97, 1.0) * sunSpec * (0.22 + motion * 0.10);
    surfaceAccent += vec3(0.78, 0.92, 1.0) * crest * (0.05 + motion * 0.05);
    surfaceAccent += vec3(1.0, 0.99, 0.96) * glitter * 0.22;

    vec3 finalColor = water + surfaceAccent * 0.30;

    if (presence > 0.01) {
        vec2 faceP = uFaceCenter - vec2(0.5);
        faceP.x *= aspect;
        vec2 faceDelta = p - faceP;
        float faceRadius = max(max(uFaceSize.x * aspect, uFaceSize.y) * 0.92, 0.18);
        float radius = length(faceDelta);
        float normalizedRadius = radius / max(faceRadius, 0.001);
        float vortexFalloff = 1.0 - smoothstep(0.08, 1.25, normalizedRadius);

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
        water += vec3(0.82, 0.95, 1.0) * arrivalRing * (0.08 + swirlStrength * 0.06);

        float polarAngle = atan(faceDelta.y, faceDelta.x);
        float spiralWave = sin(radius * 36.0 - polarAngle * 2.8 - time * (1.6 + motion * 1.8)) *
            vortexFalloff * swirlEnvelope;
        water += vec3(0.75, 0.92, 1.0) * max(spiralWave, 0.0) * (0.045 + swirlStrength * 0.045);

        vec2 normalOffset = vec2(field.y, field.z) * (0.0016 + deformation * 0.012 + motion * 0.003);
        normalOffset.x /= max(aspect, 0.001);
        vec2 swirlUv = swirledP;
        swirlUv.x /= max(aspect, 0.001);
        swirlUv += vec2(0.5);
        vec2 cameraUv = mix(uv, swirlUv, swirlEnvelope * swirlStrength * 0.70);
        cameraUv += normalOffset * (0.85 + deformation * 1.35);
        cameraUv += normalize(faceDelta + vec2(0.0001)) * arrivalRing * deformation * 0.0028;
        cameraUv = clamp(cameraUv, vec2(0.002), vec2(0.998));

        vec3 camera = texture(uInput, cameraUv).rgb;
        float cameraLuma = dot(camera, LUMA);
        camera = mix(vec3(cameraLuma), camera, 0.92 + reveal * 0.08);
        camera = mix(camera, camera * vec3(0.94, 1.01, 1.06), 0.10 + intensity * 0.05);

        vec2 faceRadiusUv = max(uFaceSize * vec2(1.15, 1.22), vec2(0.22, 0.28));
        float broadFace = softEllipse(uv, uFaceCenter, faceRadiusUv) * presence;
        float faceGuide = clamp(broadFace * 0.85, 0.0, 1.0);

        // Reveal ramp still uses cameraBlend / faceClarity, then settles to curator water↔cam mix.
        float revealBlend = 0.28 + smoothstep(0.0, 0.70, reveal) * 0.72;
        float introCamera = presence * uCameraBlend * revealBlend * (0.55 + intensity * 0.14);
        float faceIntro = presence * uFaceClarity * (0.55 + reveal * 0.40);
        float introMix = clamp(mix(introCamera, max(introCamera, faceIntro), faceGuide), 0.0, 1.0);

        float settleSum = max(settledWater + settledCamera, 0.001);
        float settleCamShare = settledCamera / settleSum;
        float settleMix = presence * settleCamShare;

        // Softly hand off from intro blend to settled water/camera weights.
        float settleWeight = smoothstep(0.45, 0.95, reveal);
        float cameraMix = mix(introMix, settleMix, settleWeight);

        // Face stays a bit clearer than the room when clarity is high.
        cameraMix = mix(cameraMix, max(cameraMix, faceGuide * uFaceClarity * settleCamShare), faceGuide * 0.35);
        cameraMix = clamp(cameraMix, 0.0, 0.97);

        float protectedMix = faceGuide * clamp(uFaceClarity, 0.0, 1.0);
        vec3 reflection = mix(camera, texture(uInput, uv).rgb, protectedMix * deformation * 0.25);
        finalColor = mix(water, reflection, cameraMix);

        // Living water overlay scales with settledWater so curator can keep motion without drowning cam.
        float livingVeil = settledWater * (0.08 + motion * 0.06 + deformation * 0.05) *
            mix(1.0, 0.45, faceGuide) * presence * settleWeight;
        finalColor = mix(finalColor, water, livingVeil);
        finalColor += surfaceAccent * mix(0.75, 0.30, faceGuide) * (0.40 + settledWater * 0.60);
        finalColor += vec3(0.85, 0.95, 1.0) * fresnel * mix(0.08, 0.03, faceGuide) * presence * settledWater;
    }

    float vignette = smoothstep(0.28, 0.95, dot(p, p) * 1.15);
    finalColor *= 1.0 - vignette * (0.025 + depth * 0.035);
    finalColor = mix(finalColor, finalColor * vec3(0.94, 0.98, 1.04), depth * 0.10);
    finalColor = mix(vec3(dot(finalColor, LUMA)), finalColor, 0.90 + intensity * 0.10);

    fragColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
}
