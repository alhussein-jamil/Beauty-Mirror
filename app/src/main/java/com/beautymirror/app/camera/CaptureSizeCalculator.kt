package com.beautymirror.app.camera

import kotlin.math.roundToInt

data class CaptureSize(val width: Int, val height: Int)

object CaptureSizeCalculator {
    /** Preserve the display aspect while capping the longest edge. Never upscale. */
    fun fitWithin(width: Int, height: Int, maxLongEdge: Int = 1920): CaptureSize {
        val safeW = width.coerceAtLeast(1)
        val safeH = height.coerceAtLeast(1)
        val limit = maxLongEdge.coerceAtLeast(1)
        val longEdge = maxOf(safeW, safeH)
        val scale = if (longEdge > limit) limit.toFloat() / longEdge else 1f
        return CaptureSize(
            width = (safeW * scale).roundToInt().coerceAtLeast(1),
            height = (safeH * scale).roundToInt().coerceAtLeast(1),
        )
    }
}
