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
    return exp(-dot(d, d) * 1.55);
}

void main() {
    vec4 base = texture(uInput, vTexCoord);
    if (uEnabled < 0.5) {
        fragColor = base;
        return;
    }

    vec2 uv = vTexCoord;
    float aspect = uViewport.x / max(uViewport.y, 1.0);

    // Aspect-correct vector from face — puddle rings expand in real screen space.
    vec2 fromFace = uv - uFaceCenter;
    fromFace.x *= aspect;
    float r = length(fromFace);
    vec2 radial = fromFace / max(r, 1e-4);

    vec2 faceRadius = max(uFaceSize * vec2(0.70, 0.68), vec2(0.12, 0.16));
    float faceCore = softEllipse(uv, uFaceCenter, faceRadius * vec2(0.70, 0.68)) * uFacePresence;
    float faceRim = softEllipse(uv, uFaceCenter, faceRadius * vec2(1.05, 1.02)) * uFacePresence;
    faceRim = clamp(faceRim - faceCore * 0.75, 0.0, 1.0);

    float reveal = smoothstep(0.02, 0.75, uVisitorReveal);
    float motion = clamp(uMotion, 0.0, 1.0);
    float intensity = clamp(uIntensity, 0.0, 1.0);
    // Motion=0 still breathes a little; motion=1 is an obvious rain-puddle.
    float life = mix(0.20, 1.0, motion);

    // --- Puddle height field (concentric expanding ripples) ---
    float freq = mix(22.0, 40.0, motion);
    float speed = mix(2.8, 8.5, motion);
    // Slow decay so rings stay readable across most of the frame.
    float damp = exp(-r * mix(2.4, 1.1, motion));
    float presenceBoost = mix(0.75, 1.0, uFacePresence);

    float phase1 = r * freq - uTime * speed;
    float phase2 = r * freq * 1.65 - uTime * speed * 1.40 + 1.1;
    float phase3 = r * freq * 0.45 - uTime * speed * 0.55 + 2.3;
    float h1 = sin(phase1);
    float h2 = sin(phase2);
    float h3 = sin(phase3);
    float height = (h1 * 0.70 + h2 * 0.40 + h3 * 0.25) * damp * presenceBoost * life;

    // Visitor drop: a stronger expanding crest when someone arrives.
    float dropR = reveal * mix(0.15, 0.95, fract(uTime * mix(0.12, 0.28, motion)));
    float drop = exp(-pow((r - dropR) * 14.0, 2.0)) * reveal * life;
    height += drop * 1.35;

    // Analytical radial slope → refraction direction (classic puddle look).
    float slope =
        (freq * cos(phase1) * 0.70 +
         freq * 1.65 * cos(phase2) * 0.40 +
         freq * 0.45 * cos(phase3) * 0.25) * damp * presenceBoost * life;
    slope += -28.0 * (r - dropR) * drop;

    // Face stays calmer; puddle outside / rim gets full kick.
    float faceCalm = mix(1.0, 0.22, faceCore * mix(0.55, 0.90, uFaceClarity));
    // Big enough UV bend to see on a phone at mid/max motion (~2–5% UV).
    float bend = mix(0.012, 0.055, motion) * mix(0.65, 1.0, intensity) * faceCalm;
    vec2 drift = radial * slope * bend * 0.018;
    // Slight tangential swirl so crests don't look like pure zoom pulses.
    vec2 tangent = vec2(-radial.y, radial.x);
    drift += tangent * height * bend * 0.35;
    drift.x /= aspect;
    vec2 sampleUv = clamp(uv + drift, vec2(0.001), vec2(0.999));

    vec3 reflected = texture(uInput, sampleUv).rgb;
    if (uQuality > 0.45) {
        vec2 smear = radial * (0.0015 + bend * 0.04);
        smear.x /= aspect;
        vec3 a = texture(uInput, clamp(sampleUv + smear, vec2(0.001), vec2(0.999))).rgb;
        vec3 b = texture(uInput, clamp(sampleUv - smear, vec2(0.001), vec2(0.999))).rgb;
        reflected = mix(reflected, (reflected * 2.0 + a + b) * 0.25, 0.22 * life);
    }

    // Keep lots of warped image in the water so ripples actually read.
    float luma = dot(reflected, LUMA);
    vec3 abyss = vec3(0.04, 0.05, 0.055);
    vec3 peat = vec3(0.08, 0.10, 0.07);
    float depthGrad = smoothstep(0.05, 0.95, uv.y);
    float openWater = smoothstep(0.18, 0.85, r / max(length(faceRadius), 0.2));
    vec3 waterBody = mix(abyss, peat, depthGrad * 0.5 + openWater * 0.25);
    float absorb = (0.16 + uDarkness * 0.34) * mix(0.35, 1.0, openWater);
    absorb *= (1.0 - faceCore * mix(0.55, 0.85, uFaceClarity));
    vec3 toned = mix(reflected, waterBody + vec3(luma) * vec3(0.22, 0.26, 0.22), absorb);

    // Crest lighting — this is what sells "puddle" even on dark water.
    float crest = pow(clamp(0.5 + 0.5 * height, 0.0, 1.0), 2.2);
    float trough = pow(clamp(0.5 - 0.5 * height, 0.0, 1.0), 2.0);
    toned += vec3(0.70, 0.74, 0.68) * crest * (0.10 + 0.22 * intensity) * life;
    toned *= 1.0 - trough * (0.08 + 0.10 * uDarkness) * life;
    // Specular glints on steep slopes.
    toned += vec3(0.85, 0.88, 0.82) * pow(clamp(abs(slope) * 0.04, 0.0, 1.0), 2.5)
        * (0.06 + 0.14 * intensity) * life;

    float grain = hash12(floor(uv * vec2(160.0, 110.0)) + floor(uTime * 8.0)) - 0.5;
    toned += grain * 0.012 * absorb;

    // Clear face core with wet refraction that still carries ripples at the rim.
    float portal = faceCore * mix(0.42, 0.72, uFaceClarity) * mix(0.45, 1.0, reveal);
    float wetMix = mix(0.45, 0.18, uFaceClarity) * life + 0.08;
    vec3 mirrorFace = mix(base.rgb, reflected, wetMix);
    mirrorFace = mix(mirrorFace, base.rgb * vec3(1.025, 1.02, 1.01) + vec3(0.008), 0.30 + 0.35 * uFaceClarity);
    mirrorFace = mix(mirrorFace, toned, faceRim * (0.25 + 0.35 * life));
    vec3 color = mix(toned, mirrorFace, portal);

    // Rim meniscus catches crest light.
    color += vec3(0.55, 0.58, 0.52) * faceRim * crest * 0.12 * life;

    vec2 well = uv - vec2(0.5);
    well.x *= aspect;
    float vignette = smoothstep(0.14, 1.0, dot(well, well) * 1.45);
    color *= 1.0 - vignette * (0.08 + 0.18 * uDarkness);

    fragColor = vec4(clamp(color, 0.0, 1.0), base.a);
}
