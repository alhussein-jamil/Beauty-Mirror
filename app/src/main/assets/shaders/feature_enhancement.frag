#version 300 es
precision highp float;
in vec2 vTexCoord;
uniform sampler2D uInput;
uniform sampler2D uEyeMask;
uniform sampler2D uBrowMask;
uniform sampler2D uLipMask;
uniform sampler2D uMouthMask;
uniform sampler2D uFaceMask;
uniform vec2 uLeftCheek;
uniform vec2 uRightCheek;
uniform vec2 uLeftJaw;
uniform vec2 uRightJaw;
uniform vec2 uNoseCenter;
uniform vec2 uMouthCenter;
uniform float uFaceWidth;
uniform float uFaceHeight;
uniform vec2 uFaceAxisX;
uniform vec2 uFaceAxisY;
uniform float uLeftVisibility;
uniform float uRightVisibility;
uniform float uPoseWeight;
uniform float uEyeBrightening;
uniform float uBrowDefinition;
uniform float uTeethWhitening;
uniform float uLipEnhancement;
uniform float uLipTintStrength;
uniform float uLipDefinition;
uniform float uLipGloss;
uniform float uBlush;
uniform float uContour;
uniform float uOpacity;
uniform float uEnabled;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

float saturation(vec3 c) {
    return max(c.r, max(c.g, c.b)) - min(c.r, min(c.g, c.b));
}

float orientedGaussian(vec2 uv, vec2 center, vec2 radius) {
    vec2 delta = uv - center;
    vec2 local = vec2(dot(delta, uFaceAxisX), dot(delta, uFaceAxisY));
    vec2 d = local / max(radius, vec2(1e-4));
    return exp(-dot(d, d) * 2.2);
}

vec3 preserveLumaTint(vec3 color, vec3 tint, float amount) {
    float l = dot(color, LUMA);
    vec3 mixed = mix(color, tint * max(l, 0.16), amount);
    float outL = dot(mixed, LUMA);
    return clamp(mixed + vec3(l - outL), 0.0, 1.0);
}

void main() {
    vec4 original = texture(uInput, vTexCoord);
    if (uEnabled < 0.5 || uOpacity < 0.001) {
        fragColor = original;
        return;
    }

    vec3 color = original.rgb;
    float opacity = clamp(uOpacity, 0.0, 1.0);
    float eye = texture(uEyeMask, vTexCoord).r * opacity;
    float brow = texture(uBrowMask, vTexCoord).r * opacity;
    float lip = texture(uLipMask, vTexCoord).r * opacity;
    float mouth = texture(uMouthMask, vTexCoord).r * opacity;
    float face = texture(uFaceMask, vTexCoord).r * opacity;
    float luma = dot(color, LUMA);
    float sat = saturation(color);

    float scleraCandidate = eye * smoothstep(0.28, 0.72, luma) * (1.0 - smoothstep(0.10, 0.34, sat));
    vec3 cleanWhite = mix(color, vec3(max(luma, 0.58), max(luma, 0.59), max(luma, 0.61)),
        clamp(scleraCandidate * uEyeBrightening * 1.05, 0.0, 0.65));
    color = clamp(cleanWhite, 0.0, 1.0);

    // Brow definition reinforces existing dark hair/edge energy only inside the brow masks.
    luma = dot(color, LUMA);
    float browHair = brow * (1.0 - smoothstep(0.28, 0.72, luma));
    float browAmount = clamp(browHair * uBrowDefinition * 0.34, 0.0, 0.30);
    color = mix(color, color * vec3(0.78, 0.74, 0.72), browAmount);

    luma = dot(color, LUMA);
    sat = saturation(color);
    float teethCandidate = mouth * smoothstep(0.36, 0.68, luma) * (1.0 - smoothstep(0.16, 0.48, sat));
    vec3 toothTarget = vec3(min(1.0, luma + 0.12), min(1.0, luma + 0.13), min(1.0, luma + 0.15));
    color = mix(color, toothTarget,
        clamp(teethCandidate * uTeethWhitening * 1.05, 0.0, 0.72));

    // Lips: enhance natural chroma/shape — never paint a fixed lipstick color.
    float lipBody = smoothstep(0.10, 0.78, lip);
    float lipLuma = dot(color, LUMA);
    // Lift existing lip chroma toward a richer version of itself.
    vec3 lipChroma = color - vec3(lipLuma);
    float chromaBoost = 1.0 + uLipEnhancement * 0.85;
    vec3 lipEnhanced = vec3(lipLuma) + lipChroma * chromaBoost;
    // Soft fullness: slight midtone darken inside the vermilion body.
    lipEnhanced *= 1.0 - lipBody * uLipEnhancement * 0.06;
    color = mix(color, clamp(lipEnhanced, 0.0, 1.0), clamp(lipBody * uLipEnhancement * 0.95, 0.0, 0.72));

    // Optional warm tint biased by the pixel's own hue (not a rose overlay).
    float warmBias = clamp(uLipTintStrength, 0.0, 1.0);
    if (warmBias > 0.001 && lipBody > 0.02) {
        vec3 warmSelf = color * vec3(1.06, 0.97, 0.94);
        float warmAmt = clamp(lipBody * warmBias * 0.70, 0.0, 0.45);
        color = mix(color, warmSelf, warmAmt);
        float warmL = dot(color, LUMA);
        color += vec3(lipLuma - warmL) * warmAmt;
        color = clamp(color, 0.0, 1.0);
    }

    // Lip definition: reinforce the vermilion border with local contrast, not pink paint.
    float lipEdge = clamp(smoothstep(0.02, 0.18, lip) - smoothstep(0.42, 0.88, lip), 0.0, 1.0);
    float edgeAmt = clamp(lipEdge * uLipDefinition, 0.0, 1.0);
    vec3 edgeDefined = color * (1.0 - edgeAmt * 0.16);
    edgeDefined += (color - vec3(dot(color, LUMA))) * edgeAmt * 0.35;
    color = mix(color, clamp(edgeDefined, 0.0, 1.0), clamp(uLipDefinition * 0.90, 0.0, 1.0) * lipEdge);

    // Lip gloss: subtle central highlight aligned with the mouth.
    float gloss = orientedGaussian(
        vTexCoord,
        uMouthCenter + uFaceAxisY * (-uFaceHeight * 0.015),
        vec2(max(uFaceWidth * 0.16, 0.03), max(uFaceHeight * 0.035, 0.012))
    ) * lipBody * (0.45 + 0.55 * smoothstep(0.22, 0.75, lipLuma));
    color += vec3(gloss * uLipGloss * 0.16);

    float pose = clamp(uPoseWeight, 0.0, 1.0);
    vec2 cheekRadius = vec2(max(uFaceWidth * 0.15, 0.025), max(uFaceHeight * 0.095, 0.025));
    float leftBlush = orientedGaussian(vTexCoord, uLeftCheek, cheekRadius) * uLeftVisibility;
    float rightBlush = orientedGaussian(vTexCoord, uRightCheek, cheekRadius) * uRightVisibility;
    float blush = (leftBlush + rightBlush) * face * pose * uBlush;
    // Warm the cheek's own color slightly instead of stamping a fixed blush pigment.
    vec3 blushWarm = color * vec3(1.05, 0.96, 0.94);
    color = mix(color, blushWarm, clamp(blush * 0.42, 0.0, 0.38));

    vec2 jawRadius = vec2(max(uFaceWidth * 0.19, 0.035), max(uFaceHeight * 0.15, 0.035));
    float jawShade = (
        orientedGaussian(vTexCoord, uLeftJaw, jawRadius) * uLeftVisibility +
        orientedGaussian(vTexCoord, uRightJaw, jawRadius) * uRightVisibility
    ) * face * pose;
    float noseLight = orientedGaussian(
        vTexCoord,
        uNoseCenter,
        vec2(max(uFaceWidth * 0.075, 0.016), max(uFaceHeight * 0.18, 0.035))
    ) * face * pose;
    float contour = clamp(uContour, 0.0, 1.0);
    color *= 1.0 - jawShade * contour * 0.18;
    color += vec3(noseLight * contour * 0.08);

    fragColor = vec4(clamp(color, 0.0, 1.0), original.a);
}
