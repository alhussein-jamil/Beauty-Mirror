#version 300 es
precision mediump float;
in vec2 vTexCoord;
uniform sampler2D uInput;
uniform sampler2D uOriginal;
uniform sampler2D uEyeMask;
uniform sampler2D uDetailMask;
uniform float uEyeClarity;
uniform float uDetailPreservation;
uniform float uEyeSparkle;
uniform float uEnabled;
uniform vec2 uTexelSize;
out vec4 fragColor;

void main() {
    vec4 current = texture(uInput, vTexCoord);
    if (uEnabled < 0.5) {
        fragColor = current;
        return;
    }
    vec3 orig = texture(uOriginal, vTexCoord).rgb;
    float eye = texture(uEyeMask, vTexCoord).r;
    float detail = texture(uDetailMask, vTexCoord).r;
    // Unsharp against the untreated original so eyes regain real iris/lash edge energy.
    vec3 blurOrig = (
        texture(uOriginal, vTexCoord + vec2(uTexelSize.x, 0.0)).rgb +
        texture(uOriginal, vTexCoord - vec2(uTexelSize.x, 0.0)).rgb +
        texture(uOriginal, vTexCoord + vec2(0.0, uTexelSize.y)).rgb +
        texture(uOriginal, vTexCoord - vec2(0.0, uTexelSize.y)).rgb
    ) * 0.25;
    vec3 eyeBoost = current.rgb + (orig - blurOrig) * (1.18 * uEyeClarity * eye);
    float originalLuma = dot(orig, vec3(0.2126, 0.7152, 0.0722));
    float sparkleGate = eye * smoothstep(0.42, 0.86, originalLuma);
    eyeBoost += vec3(sparkleGate * uEyeSparkle * 0.075);
    // Restore protected feature texture from original (brows/lips/eyes).
    float restore = clamp(detail * uDetailPreservation, 0.0, 1.0);
    vec3 mixed = mix(eyeBoost, mix(eyeBoost, orig, 0.28), restore);
    fragColor = vec4(clamp(mixed, 0.0, 1.0), current.a);
}
