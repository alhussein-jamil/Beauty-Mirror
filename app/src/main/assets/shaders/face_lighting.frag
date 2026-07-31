#version 300 es
precision highp float;
in vec2 vTexCoord;
uniform sampler2D uInput;
uniform sampler2D uFaceMask;
uniform float uFaceExposure;
uniform float uShadowLift;
uniform float uHighlightProtection;
uniform float uLocalContrast;
uniform float uFaceLuma;
uniform float uShineControl;
uniform float uSkinGlow;
uniform float uEnabled;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

float toneCurve(float x, float exposure, float shadowLift, float highlightProtection) {
    float exposed = x * exp2(exposure);
    float lifted = exposed + shadowLift * (1.0 - exposed) * (1.0 - exposed) * 0.55;
    float highlightWeight = smoothstep(0.60, 1.0, lifted);
    float compressed = mix(lifted, lifted / (1.0 + highlightProtection * lifted), highlightWeight);
    return clamp(compressed, 0.0, 1.0);
}

void main() {
    vec4 original = texture(uInput, vTexCoord);
    if (uEnabled < 0.5) {
        fragColor = original;
        return;
    }

    float mask = texture(uFaceMask, vTexCoord).r;
    if (mask < 0.001) {
        fragColor = original;
        return;
    }

    float luma = dot(original.rgb, LUMA);
    float shaped = toneCurve(luma, uFaceExposure, uShadowLift, uHighlightProtection);
    float pivot = clamp(uFaceLuma, 0.18, 0.82);
    float contrasted = (shaped - pivot) * (1.0 + uLocalContrast * 0.45) + pivot;
    float outputLuma = mix(shaped, contrasted, clamp(uLocalContrast, 0.0, 1.0));

    // Shine control compresses only the upper luminance shoulder instead of dimming the face.
    float shineGate = smoothstep(0.68, 0.95, outputLuma);
    float matteLuma = mix(outputLuma, 0.70 + (outputLuma - 0.70) * 0.34, shineGate);
    outputLuma = mix(outputLuma, matteLuma, clamp(uShineControl, 0.0, 1.0));

    // Glow is a restrained mid-tone lift; highlights remain protected by the shoulder above.
    float glowGate = smoothstep(0.16, 0.48, outputLuma) * (1.0 - smoothstep(0.70, 0.93, outputLuma));
    outputLuma += glowGate * uSkinGlow * 0.055;

    vec3 chroma = original.rgb - vec3(luma);
    vec3 corrected = vec3(outputLuma) + chroma;

    // Retain specular highlights and use only the semantic face mask. No automatic target
    // brightness is derived from skin tone; the same user control applies across skin tones.
    float specular = smoothstep(0.78, 0.96, luma);
    float amount = mask * (1.0 - specular * 0.82);
    fragColor = vec4(mix(original.rgb, clamp(corrected, 0.0, 1.0), amount), original.a);
}
