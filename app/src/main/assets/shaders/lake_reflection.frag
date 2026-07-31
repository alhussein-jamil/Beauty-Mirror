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

float softEllipse(vec2 uv, vec2 center, vec2 radius) {
    vec2 d = (uv - center) / max(radius, vec2(0.001));
    return exp(-dot(d, d) * 1.65);
}

void main() {
    vec4 base = texture(uInput, vTexCoord);
    if (uEnabled < 0.5) {
        fragColor = base;
        return;
    }

    float aspect = uViewport.x / max(uViewport.y, 1.0);
    float speed = mix(0.10, 0.48, uMotion);
    float t = uTime * speed;
    vec2 uv = vTexCoord;

    // Horizontal, low-amplitude water movement: ripples rather than sea waves.
    float broad = sin(uv.y * 48.0 + t * 2.2 + sin(uv.y * 11.0 - t * 0.7) * 0.9);
    float fine = sin(uv.y * 109.0 - t * 3.1 + uv.x * 8.0) * 0.42;
    float drift = sin(uv.y * 19.0 + uv.x * 5.0 + t * 0.9) * 0.26;

    vec2 faceDelta = uv - uFaceCenter;
    faceDelta.x *= aspect;
    float faceDistance = length(faceDelta / max(vec2(uFaceSize.x * aspect, uFaceSize.y), vec2(0.08)));
    float revealPulse = sin(clamp(uVisitorReveal, 0.0, 1.0) * 3.14159265);
    float arrivalRipple = sin(faceDistance * 19.0 - t * 4.0) * exp(-faceDistance * 1.9) *
        uFacePresence * (0.36 + revealPulse * 0.92);

    float displacement = (broad + fine + drift + arrivalRipple * 0.75) *
        mix(0.0016, 0.0060, uIntensity);
    vec2 offset = vec2(displacement, displacement * 0.16 * cos(uv.x * 15.0 + t));
    vec2 sampleUv = clamp(uv + offset, vec2(0.002), vec2(0.998));

    vec3 color = texture(uInput, sampleUv).rgb;
    if (uQuality > 0.42) {
        vec2 tap = vec2(abs(displacement) * 0.42 + 0.00045, 0.00035);
        vec3 sideA = texture(uInput, clamp(sampleUv + tap, vec2(0.002), vec2(0.998))).rgb;
        vec3 sideB = texture(uInput, clamp(sampleUv - tap, vec2(0.002), vec2(0.998))).rgb;
        color = mix(color, (color * 2.0 + sideA + sideB) * 0.25, 0.34 * uIntensity);
    }

    float luma = dot(color, LUMA);
    float faceWindow = softEllipse(
        uv,
        uFaceCenter,
        max(uFaceSize * vec2(0.72, 0.68), vec2(0.12, 0.16))
    ) * uFacePresence * smoothstep(0.04, 0.88, uVisitorReveal);

    // Dark olive/charcoal/brown water palette; deliberately avoids pool turquoise.
    vec3 lakeNeutral = vec3(luma * 0.58, luma * 0.64, luma * 0.56);
    lakeNeutral += vec3(0.018, 0.024, 0.011) * (1.0 - luma);
    float paletteAmount = uIntensity * mix(0.28, 0.56, uDarkness);
    color = mix(color, lakeNeutral, paletteAmount);

    // Preserve the face correction inside the water treatment while keeping it visibly embedded.
    float faceRelief = faceWindow * uFaceClarity * mix(0.10, 0.34, uVisitorReveal);
    float darken = uDarkness * uIntensity * (0.34 - faceRelief);
    color *= 1.0 - clamp(darken, 0.0, 0.42);
    color = mix(
        color,
        color * mix(1.02, 1.10, uVisitorReveal) + vec3(0.012),
        faceWindow * uFaceClarity * mix(0.05, 0.22, uVisitorReveal)
    );

    // Sparse silver-brown surface glints, strongest away from the face.
    float ridge = smoothstep(0.86, 0.985, 0.5 + 0.5 * sin(uv.y * 82.0 + t * 2.8 + broad));
    float glint = ridge * (1.0 - faceWindow * 0.55) * uIntensity * 0.055;
    color += vec3(0.62, 0.61, 0.50) * glint;

    vec2 centered = uv - vec2(0.5);
    float vignette = smoothstep(0.28, 0.78, dot(centered, centered) * 1.55);
    color *= 1.0 - vignette * uIntensity * 0.16;

    fragColor = vec4(clamp(color, 0.0, 1.0), base.a);
}
