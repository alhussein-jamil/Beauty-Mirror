package com.beautymirror.app.masks

import com.beautymirror.app.tracking.LandmarkPoint
import kotlin.math.hypot

/**
 * MediaPipe Face Landmarker topology helpers (478 landmarks).
 * Indices follow the canonical Face Mesh / Face Landmarker set.
 */
object FaceMaskGeometry {
    /** Face oval contour indices (approximate outer face oval). */
    val FACE_OVAL = intArrayOf(
        10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288,
        397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136,
        172, 58, 132, 93, 234, 127, 162, 21, 54, 103, 67, 109,
    )

    val LEFT_EYE = intArrayOf(33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246)
    val RIGHT_EYE = intArrayOf(263, 249, 390, 373, 374, 380, 381, 382, 362, 398, 384, 385, 386, 387, 388, 466)

    val LEFT_EYEBROW = intArrayOf(70, 63, 105, 66, 107, 55, 65, 52, 53, 46)
    val RIGHT_EYEBROW = intArrayOf(300, 293, 334, 296, 336, 285, 295, 282, 283, 276)

    val LIPS_OUTER = intArrayOf(61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291, 308, 324, 318, 402, 317, 14, 87, 178, 88, 95)
    val LIPS_INNER = intArrayOf(78, 95, 88, 178, 87, 14, 317, 402, 318, 324, 308)

    val LEFT_IRIS = intArrayOf(468, 469, 470, 471, 472)
    val RIGHT_IRIS = intArrayOf(473, 474, 475, 476, 477)

    val LEFT_LOWER_EYE = intArrayOf(33, 7, 163, 144, 145, 153, 154, 155, 133)
    val RIGHT_LOWER_EYE = intArrayOf(263, 249, 390, 373, 374, 380, 381, 382, 362)

    val NOSTRILS = intArrayOf(98, 97, 2, 326, 327)

    fun safePoints(landmarks: List<LandmarkPoint>, indices: IntArray): List<LandmarkPoint> {
        if (landmarks.isEmpty()) return emptyList()
        val out = ArrayList<LandmarkPoint>(indices.size)
        for (i in indices) {
            landmarks.getOrNull(i)?.let { out.add(it) }
        }
        return out
    }


    fun expandPolygonInto(points: List<LandmarkPoint>, amount: Float, dest: MutableList<LandmarkPoint>) {
        dest.clear()
        if (points.size < 3 || amount == 0f) {
            dest.addAll(points)
            return
        }
        var cx = 0f
        var cy = 0f
        for (p in points) {
            cx += p.x
            cy += p.y
        }
        val inv = 1f / points.size
        cx *= inv
        cy *= inv
        for (p in points) {
            val dx = p.x - cx
            val dy = p.y - cy
            val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1e-4f)
            dest.add(
                LandmarkPoint(
                    (p.x + dx / len * amount).coerceIn(0f, 1f),
                    (p.y + dy / len * amount).coerceIn(0f, 1f),
                ),
            )
        }
    }

    /** Inflate polygon radially from centroid — soft extra coverage for skin mask. */
    fun expandPolygon(points: List<LandmarkPoint>, amount: Float): List<LandmarkPoint> {
        val dest = ArrayList<LandmarkPoint>(points.size)
        expandPolygonInto(points, amount, dest)
        return dest
    }

    /**
     * @param yIncreasesDownward true for MediaPipe/display space; false for GL texture space (Y-up).
     */
    fun underEyePolygonInto(
        landmarks: List<LandmarkPoint>,
        left: Boolean,
        yIncreasesDownward: Boolean,
        faceWidthNorm: Float,
        dest: MutableList<LandmarkPoint>,
    ) {
        dest.clear()
        val indices = if (left) LEFT_LOWER_EYE else RIGHT_LOWER_EYE
        // Fill lower-lid points into dest first (no temporary list).
        for (i in indices) {
            landmarks.getOrNull(i)?.let { dest.add(it) }
        }
        if (dest.size < 3) {
            dest.clear()
            return
        }
        val lowerCount = dest.size
        val faceW = if (faceWidthNorm > 0f) faceWidthNorm else {
            var minX = 1f; var maxX = 0f
            for (p in landmarks) {
                if (p.x < minX) minX = p.x
                if (p.x > maxX) maxX = p.x
            }
            maxX - minX
        }
        val drop = (faceW * 0.085f).coerceIn(0.02f, 0.08f)
        for (i in (lowerCount - 1) downTo 0) {
            val p = dest[i]
            val y = if (yIncreasesDownward) {
                (p.y + drop).coerceAtMost(1f)
            } else {
                (p.y - drop).coerceAtLeast(0f)
            }
            dest.add(LandmarkPoint(p.x, y))
        }
    }

    fun underEyePolygon(
        landmarks: List<LandmarkPoint>,
        left: Boolean,
        yIncreasesDownward: Boolean = true,
        faceWidthNorm: Float = -1f,
    ): List<LandmarkPoint> {
        val dest = ArrayList<LandmarkPoint>(32)
        underEyePolygonInto(landmarks, left, yIncreasesDownward, faceWidthNorm, dest)
        return dest
    }
}
