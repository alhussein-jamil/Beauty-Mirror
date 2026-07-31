package com.beautymirror.app.rendering

/**
 * Texture-coordinate scale for a centered aspect-fill crop.
 * Values are in (0, 1]; the shader samples around the texture center.
 */
data class AspectCrop(
    val scaleX: Float,
    val scaleY: Float,
) {
    companion object {
        /**
         * Computes an aspect-fill crop in the orientation in which the camera frame is shown.
         * CameraX commonly provides a landscape buffer for a portrait display. The
         * SurfaceTexture transform rotates the pixels, so the source dimensions must be
         * swapped for 90/270 degree rotations before comparing aspect ratios.
         */
        fun centerCrop(
            sourceWidth: Int,
            sourceHeight: Int,
            destinationWidth: Int,
            destinationHeight: Int,
            rotationDegrees: Int = 0,
        ): AspectCrop {
            val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
            val swapsAxes = normalizedRotation == 90 || normalizedRotation == 270
            val orientedWidth = if (swapsAxes) sourceHeight else sourceWidth
            val orientedHeight = if (swapsAxes) sourceWidth else sourceHeight

            val srcW = orientedWidth.coerceAtLeast(1).toFloat()
            val srcH = orientedHeight.coerceAtLeast(1).toFloat()
            val dstW = destinationWidth.coerceAtLeast(1).toFloat()
            val dstH = destinationHeight.coerceAtLeast(1).toFloat()
            val sourceAspect = srcW / srcH
            val destinationAspect = dstW / dstH
            return when {
                sourceAspect > destinationAspect -> AspectCrop(
                    scaleX = (destinationAspect / sourceAspect).coerceIn(0.01f, 1f),
                    scaleY = 1f,
                )
                sourceAspect < destinationAspect -> AspectCrop(
                    scaleX = 1f,
                    scaleY = (sourceAspect / destinationAspect).coerceIn(0.01f, 1f),
                )
                else -> AspectCrop(1f, 1f)
            }
        }
    }
}
