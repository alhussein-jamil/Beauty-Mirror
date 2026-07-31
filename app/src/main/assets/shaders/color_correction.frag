#version 300 es
precision highp float;
in vec2 vTexCoord;
uniform sampler2D uInput;
uniform sampler2D uFaceMask;
uniform float uWarmth;
uniform float uLocalContrast;
uniform float uEnabled;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

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
    vec3 chroma = original.rgb - vec3(luma);
    chroma += vec3(0.030, 0.010, -0.024) * uWarmth;
    float contrasted = (luma - 0.5) * (1.0 + uLocalContrast * 0.5) + 0.5;
    vec3 corrected = vec3(contrasted) + chroma;
    fragColor = vec4(mix(original.rgb, clamp(corrected, 0.0, 1.0), mask), original.a);
}
