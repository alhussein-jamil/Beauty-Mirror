package com.beautymirror.app.rendering

/**
 * Resolves the extra consumer-side horizontal flip required to end up in the requested display
 * space. CameraX may already request mirroring through transformation metadata for front camera
 * preview. Applying a second explicit flip in that case would undo the mirror and make left/right
 * movement feel reversed.
 */
object MirrorTransform {
    fun extraFlip(
        desiredMirror: Boolean,
        surfaceContainsCameraTransform: Boolean,
        cameraTransformRequestsMirror: Boolean,
    ): Boolean {
        val upstreamMirror = surfaceContainsCameraTransform && cameraTransformRequestsMirror
        return desiredMirror.xor(upstreamMirror)
    }
}
