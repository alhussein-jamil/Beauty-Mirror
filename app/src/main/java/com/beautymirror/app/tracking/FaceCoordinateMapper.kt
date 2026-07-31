package com.beautymirror.app.tracking

import com.beautymirror.app.util.MatrixUtils

/**
 * Maps MediaPipe normalized image landmarks into display and texture spaces.
 *
 * Pipeline:
 * 1. Analysis image coords (normalized, origin top-left, already upright if MediaPipe rotated)
 * 2. Center-crop to preview aspect
 * 3. Display: optional mirror (for overlays)
 * 4. Texture: the same optional mirror, then Y flipped for GL
 *
 * The camera input pass establishes the requested presentation space before every beauty pass,
 * so masks and the visible preview share one deterministic coordinate system.
 */
class FaceCoordinateMapper {
    data class Config(
        val rotationDegrees: Int = 0,
        val mirrorFront: Boolean = true,
        val analysisWidth: Int = 1,
        val analysisHeight: Int = 1,
        val previewWidth: Int = 1,
        val previewHeight: Int = 1,
        val cropLeft: Float = 0f,
        val cropTop: Float = 0f,
        val cropWidth: Float = 1f,
        val cropHeight: Float = 1f,
    )

    @Volatile
    var config: Config = Config()

    /** Shared analysis→preview mapping without mirroring. */
    fun toAligned(x: Float, y: Float, c: Config = config): LandmarkPoint {
        var (nx, ny) = MatrixUtils.mapNormalizedPoint(x, y, c.rotationDegrees, mirrorX = false)
        val srcW = if (c.rotationDegrees % 180 == 0) c.analysisWidth.toFloat() else c.analysisHeight.toFloat()
        val srcH = if (c.rotationDegrees % 180 == 0) c.analysisHeight.toFloat() else c.analysisWidth.toFloat()
        val dstW = c.previewWidth.toFloat().coerceAtLeast(1f)
        val dstH = c.previewHeight.toFloat().coerceAtLeast(1f)
        val srcAspect = srcW / srcH.coerceAtLeast(1f)
        val dstAspect = dstW / dstH
        if (srcAspect > dstAspect) {
            val scale = dstAspect / srcAspect
            val offset = (1f - scale) * 0.5f
            nx = (nx - offset) / scale
        } else {
            val scale = srcAspect / dstAspect
            val offset = (1f - scale) * 0.5f
            ny = (ny - offset) / scale
        }
        nx = (nx - c.cropLeft) / c.cropWidth.coerceAtLeast(1e-4f)
        ny = (ny - c.cropTop) / c.cropHeight.coerceAtLeast(1e-4f)
        return LandmarkPoint(nx, ny)
    }

    fun toDisplay(x: Float, y: Float, c: Config = config): LandmarkPoint {
        val aligned = toAligned(x, y, c)
        val nx = if (c.mirrorFront) 1f - aligned.x else aligned.x
        return LandmarkPoint(nx, aligned.y)
    }

    fun toTexture(x: Float, y: Float, c: Config = config): LandmarkPoint {
        val aligned = toAligned(x, y, c)
        val nx = if (c.mirrorFront) 1f - aligned.x else aligned.x
        return LandmarkPoint(nx, 1f - aligned.y)
    }

    fun mapLandmarks(
        normalized: List<LandmarkPoint>,
        c: Config = config,
    ): Pair<List<LandmarkPoint>, List<LandmarkPoint>> {
        val display = ArrayList<LandmarkPoint>(normalized.size)
        val texture = ArrayList<LandmarkPoint>(normalized.size)
        mapLandmarksInto(normalized, display, texture, c)
        return display to texture
    }

    /** Fill [displayOut]/[textureOut] without allocating new lists (caller owns reuse). */
    fun mapLandmarksInto(
        normalized: List<LandmarkPoint>,
        displayOut: MutableList<LandmarkPoint>,
        textureOut: MutableList<LandmarkPoint>,
        c: Config = config,
    ) {
        displayOut.clear()
        textureOut.clear()
        for (p in normalized) {
            displayOut.add(toDisplay(p.x, p.y, c))
            textureOut.add(toTexture(p.x, p.y, c))
        }
    }

    fun mapBounds(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        c: Config = config,
    ): FaceBounds {
        val c0 = toDisplay(left, top, c)
        val c1 = toDisplay(right, top, c)
        val c2 = toDisplay(left, bottom, c)
        val c3 = toDisplay(right, bottom, c)
        return FaceBounds(
            left = minOf(c0.x, c1.x, c2.x, c3.x),
            top = minOf(c0.y, c1.y, c2.y, c3.y),
            right = maxOf(c0.x, c1.x, c2.x, c3.x),
            bottom = maxOf(c0.y, c1.y, c2.y, c3.y),
        )
    }
}
