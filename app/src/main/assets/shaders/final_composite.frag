#version 300 es
precision mediump float;
in vec2 vTexCoord;
uniform sampler2D uProcessed;
uniform sampler2D uOriginal;
uniform float uBeforeAfter; // 1 = show original
uniform float uDim; // brief darken after param changes
out vec4 fragColor;
void main() {
    vec4 processed = texture(uProcessed, vTexCoord);
    vec4 original = texture(uOriginal, vTexCoord);
    vec4 color = mix(processed, original, clamp(uBeforeAfter, 0.0, 1.0));
    float dim = clamp(uDim, 0.0, 0.65);
    color.rgb *= 1.0 - dim;
    fragColor = color;
}
