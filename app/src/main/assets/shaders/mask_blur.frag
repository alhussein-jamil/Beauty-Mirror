#version 300 es
precision mediump float;
in vec2 vTexCoord;
uniform sampler2D uMask;
uniform vec2 uTexelSize;
uniform float uRadius;
out vec4 fragColor;

// Separable-friendly 5-tap blur (run twice with swapped texel axis for soft mask edges).
void main() {
    float r = max(uRadius, 1.0);
    vec2 step = uTexelSize * r;
    vec4 c = texture(uMask, vTexCoord);
    vec4 p1 = texture(uMask, vTexCoord + step);
    vec4 n1 = texture(uMask, vTexCoord - step);
    vec4 p2 = texture(uMask, vTexCoord + step * 2.0);
    vec4 n2 = texture(uMask, vTexCoord - step * 2.0);
    fragColor = vec4(
        c.r * 0.227027 + p1.r * 0.316216 + n1.r * 0.316216 + p2.r * 0.070270 + n2.r * 0.070270,
        c.g * 0.227027 + p1.g * 0.316216 + n1.g * 0.316216 + p2.g * 0.070270 + n2.g * 0.070270,
        c.b * 0.227027 + p1.b * 0.316216 + n1.b * 0.316216 + p2.b * 0.070270 + n2.b * 0.070270,
        c.a
    );
}
