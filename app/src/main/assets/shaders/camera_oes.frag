#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision highp float;
in vec2 vTexCoord;
uniform samplerExternalOES uTexture;
uniform mat4 uTexMatrix;
uniform vec2 uCropScale;
uniform float uMirrorX;
out vec4 fragColor;

void main() {
    // vTexCoord is in the final upright display space. Apply the optional horizontal mirror
    // here, before mapping back through SurfaceTexture, so every later pass and mask sees the
    // same mirrored coordinate system.
    vec2 displayUv = vTexCoord;
    if (uMirrorX > 0.5) {
        displayUv.x = 1.0 - displayUv.x;
    }
    vec2 cropped = (displayUv - vec2(0.5)) * uCropScale + vec2(0.5);
    vec2 uv = (uTexMatrix * vec4(cropped, 0.0, 1.0)).xy;
    fragColor = texture(uTexture, clamp(uv, 0.0, 1.0));
}
