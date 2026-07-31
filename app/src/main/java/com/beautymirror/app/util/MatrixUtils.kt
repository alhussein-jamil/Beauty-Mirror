package com.beautymirror.app.util

object MatrixUtils {
    fun setIdentity(m: FloatArray) {
        require(m.size >= 16)
        for (i in 0 until 16) m[i] = 0f
        m[0] = 1f; m[5] = 1f; m[10] = 1f; m[15] = 1f
    }

    fun multiply(result: FloatArray, lhs: FloatArray, rhs: FloatArray) {
        // Manual expand avoids allocating a temp matrix on callers that reuse [result].
        val r0 = lhs[0] * rhs[0] + lhs[1] * rhs[4] + lhs[2] * rhs[8] + lhs[3] * rhs[12]
        val r1 = lhs[0] * rhs[1] + lhs[1] * rhs[5] + lhs[2] * rhs[9] + lhs[3] * rhs[13]
        val r2 = lhs[0] * rhs[2] + lhs[1] * rhs[6] + lhs[2] * rhs[10] + lhs[3] * rhs[14]
        val r3 = lhs[0] * rhs[3] + lhs[1] * rhs[7] + lhs[2] * rhs[11] + lhs[3] * rhs[15]
        val r4 = lhs[4] * rhs[0] + lhs[5] * rhs[4] + lhs[6] * rhs[8] + lhs[7] * rhs[12]
        val r5 = lhs[4] * rhs[1] + lhs[5] * rhs[5] + lhs[6] * rhs[9] + lhs[7] * rhs[13]
        val r6 = lhs[4] * rhs[2] + lhs[5] * rhs[6] + lhs[6] * rhs[10] + lhs[7] * rhs[14]
        val r7 = lhs[4] * rhs[3] + lhs[5] * rhs[7] + lhs[6] * rhs[11] + lhs[7] * rhs[15]
        val r8 = lhs[8] * rhs[0] + lhs[9] * rhs[4] + lhs[10] * rhs[8] + lhs[11] * rhs[12]
        val r9 = lhs[8] * rhs[1] + lhs[9] * rhs[5] + lhs[10] * rhs[9] + lhs[11] * rhs[13]
        val r10 = lhs[8] * rhs[2] + lhs[9] * rhs[6] + lhs[10] * rhs[10] + lhs[11] * rhs[14]
        val r11 = lhs[8] * rhs[3] + lhs[9] * rhs[7] + lhs[10] * rhs[11] + lhs[11] * rhs[15]
        val r12 = lhs[12] * rhs[0] + lhs[13] * rhs[4] + lhs[14] * rhs[8] + lhs[15] * rhs[12]
        val r13 = lhs[12] * rhs[1] + lhs[13] * rhs[5] + lhs[14] * rhs[9] + lhs[15] * rhs[13]
        val r14 = lhs[12] * rhs[2] + lhs[13] * rhs[6] + lhs[14] * rhs[10] + lhs[15] * rhs[14]
        val r15 = lhs[12] * rhs[3] + lhs[13] * rhs[7] + lhs[14] * rhs[11] + lhs[15] * rhs[15]
        result[0] = r0; result[1] = r1; result[2] = r2; result[3] = r3
        result[4] = r4; result[5] = r5; result[6] = r6; result[7] = r7
        result[8] = r8; result[9] = r9; result[10] = r10; result[11] = r11
        result[12] = r12; result[13] = r13; result[14] = r14; result[15] = r15
    }

    /** Maps normalized image coords through rotation degrees CW and optional mirror. */
    fun mapNormalizedPoint(
        x: Float,
        y: Float,
        rotationDegrees: Int,
        mirrorX: Boolean,
    ): Pair<Float, Float> {
        var nx = x
        var ny = y
        when (((rotationDegrees % 360) + 360) % 360) {
            90 -> {
                val t = nx
                nx = 1f - ny
                ny = t
            }
            180 -> {
                nx = 1f - nx
                ny = 1f - ny
            }
            270 -> {
                val t = nx
                nx = ny
                ny = 1f - t
            }
        }
        if (mirrorX) nx = 1f - nx
        return nx to ny
    }

    fun cropScaleToFit(
        srcWidth: Float,
        srcHeight: Float,
        dstWidth: Float,
        dstHeight: Float,
    ): FloatArray {
        // Returns 3x3-ish as 4x4 affine: scale+translate to center-crop src into dst.
        val srcAspect = srcWidth / srcHeight
        val dstAspect = dstWidth / dstHeight
        val out = FloatArray(16)
        setIdentity(out)
        if (srcAspect > dstAspect) {
            val scale = dstAspect / srcAspect
            out[0] = scale
            out[12] = (1f - scale) * 0.5f
        } else {
            val scale = srcAspect / dstAspect
            out[5] = scale
            out[13] = (1f - scale) * 0.5f
        }
        return out
    }

    /**
     * Inverse of CameraX/MediaPipe upright rotation: upright normalized → sensor buffer normalized.
     * CameraX rotationDegrees is the CW rotation applied to make the buffer upright.
     */
    fun uprightNormalizedToSensor(x: Float, y: Float, rotationDegrees: Int): Pair<Float, Float> {
        return when (((rotationDegrees % 360) + 360) % 360) {
            90 -> y to (1f - x)
            180 -> (1f - x) to (1f - y)
            270 -> (1f - y) to x
            else -> x to y
        }
    }
}
