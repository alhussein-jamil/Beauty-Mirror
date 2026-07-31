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
    return exp(-dot(d, d) * 1.75);
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
    vec2 faceRadius = max(uFaceSize * vec2(0.74, 0.72), vec2(0.12, 0.16));
    float faceMask = softEllipse(uv, uFaceCenter, faceRadius) * uFacePresence;
    float faceMaskTight = softEllipse(uv, uFaceCenter, faceRadius * vec2(0.90, 0.88)) * uFacePresence;

    vec2 norm = faceDelta / max(vec2(faceRadius.x * aspect, faceRadius.y), vec2(0.001));
    float dist = length(norm);
    float reveal = smoothstep(0.04, 0.92, uVisitorReveal);
    float portal = faceMask * (0.34 + uFaceClarity * 0.66) * reveal;
    float moat = smoothstep(0.42, 0.88, dist) * (1.0 - smoothstep(0.88, 1.34, dist));
    float outerWater = 1.0 - smoothstep(0.88, 1.50, dist);
    outerWater = 1.0 - outerWater;

    float speed = mix(0.07, 0.24, uMotion);
    float t = uTime * speed;
    float fieldA = sin(uv.y * 26.0 + t * 2.4 + sin(uv.x * 4.0 - t * 0.6));
    float fieldB = sin(uv.y * 58.0 - t * 3.1 + uv.x * 6.0) * 0.52;
    float fieldC = sin((uv.x + uv.y) * 18.0 + t * 1.1) * 0.26;
    float radial = sin(dist * 16.0 - t * 5.0) * moat;
    float arrival = sin(dist * 19.5 - t * 6.6) * exp(-dist * 1.7) * reveal * uFacePresence;
    float flow = fieldA * 0.55 + fieldB * 0.30 + fieldC * 0.15 + radial * 0.40 + arrival * 0.45;

    float refraction = mix(0.0007, 0.0048, uIntensity) * (0.42 + 0.58 * (moat + outerWater * 0.65));
    refraction *= (1.0 - portal * 0.82);
    vec2 drift = vec2(flow, fieldB * 0.55 + radial * 0.45);
    drift.x *= 1.00;
    drift.y *= 0.26;
    vec2 sampleUv = clamp(uv + drift * refraction, vec2(0.002), vec2(0.998));

    vec3 color = texture(uInput, sampleUv).rgb;
    if (uQuality > 0.45) {
        vec2 tap = vec2(abs(refraction) * 0.75 + 0.00035, 0.00028);
        vec3 sideA = texture(uInput, clamp(sampleUv + tap, vec2(0.002), vec2(0.998))).rgb;
        vec3 sideB = texture(uInput, clamp(sampleUv - tap, vec2(0.002), vec2(0.998))).rgb;
        color = mix(color, (color * 2.0 + sideA + sideB) * 0.25, 0.24 + 0.14 * uIntensity);
    }

    float luma = dot(color, LUMA);
    float grain = hash12(floor(uv * vec2(240.0, 160.0)) + floor(t * 8.0)) - 0.5;
    float depthGrad = smoothstep(0.04, 0.96, uv.y);
    vec3 peat = vec3(0.11, 0.12, 0.10);
    vec3 olive = vec3(0.14, 0.17, 0.12);
    vec3 graphite = vec3(0.09, 0.10, 0.11);
    vec3 waterTint = mix(peat, olive, depthGrad * 0.46 + 0.20) + graphite * 0.28;
    vec3 toned = mix(color, waterTint + vec3(luma) * vec3(0.26, 0.31, 0.24), 0.46 + 0.38 * uDarkness);
    toned += grain * 0.018 * (0.35 + uDarkness * 0.65);

    float edgeRings = smoothstep(0.78, 0.98, 0.5 + 0.5 * sin(dist * 28.0 - t * 5.2));
    float ringMask = moat * (0.26 + 0.74 * reveal);
    vec3 silverBrown = vec3(0.78, 0.77, 0.68);
    toned += silverBrown * edgeRings * ringMask * (0.06 + 0.14 * uIntensity);

    float outsideDarken = (0.22 + uDarkness * 0.36) * (0.38 + outerWater * 0.62);
    toned *= 1.0 - outsideDarken * (1.0 - faceMaskTight * 0.60);

    vec3 readableFace = mix(base.rgb, color, 0.12);
    readableFace = mix(readableFace, base.rgb * 1.05 + vec3(0.012), 0.38 + 0.22 * uFaceClarity);
    readableFace += (readableFace - vec3(dot(readableFace, LUMA))) * 0.10;
    color = mix(toned, readableFace, portal);

    float halo = smoothstep(0.18, 0.78, faceMask) * (1.0 - smoothstep(0.76, 1.02, dist));
    color += vec3(0.06, 0.05, 0.03) * halo * 0.12 * uIntensity;

    vec2 centered = uv - vec2(0.5);
    centered.x *= aspect;
    float vignette = smoothstep(0.18, 0.92, dot(centered, centered) * 1.4);
    color *= 1.0 - vignette * (0.06 + 0.12 * uDarkness);

    fragColor = vec4(clamp(color, 0.0, 1.0), base.a);
}
