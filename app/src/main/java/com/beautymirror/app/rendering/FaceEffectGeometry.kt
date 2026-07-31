package com.beautymirror.app.rendering

import com.beautymirror.app.tracking.LandmarkPoint
import kotlin.math.abs
import kotlin.math.hypot

/** Landmark-derived, face-local anchors for geometry and feature shaders. */
data class FaceEffectGeometry(
    val leftEye: LandmarkPoint,
    val rightEye: LandmarkPoint,
    val leftCheek: LandmarkPoint,
    val rightCheek: LandmarkPoint,
    val leftJaw: LandmarkPoint,
    val rightJaw: LandmarkPoint,
    val noseCenter: LandmarkPoint,
    val noseLeft: LandmarkPoint,
    val noseRight: LandmarkPoint,
    val mouthCenter: LandmarkPoint,
    val faceCenter: LandmarkPoint,
    val faceAxisX: LandmarkPoint,
    val faceAxisY: LandmarkPoint,
    val faceWidth: Float,
    val faceHeight: Float,
    val leftVisibility: Float,
    val rightVisibility: Float,
    val poseWeight: Float,
) {
    companion object {
        fun from(
            landmarks: List<LandmarkPoint>,
            yawDegrees: Float,
            pitchDegrees: Float,
        ): FaceEffectGeometry? {
            if (landmarks.size <= 454) return null
            fun p(index: Int) = landmarks[index]
            fun avg(vararg indices: Int): LandmarkPoint {
                var x = 0f; var y = 0f; var z = 0f; var count = 0
                for (i in indices) {
                    val point = landmarks.getOrNull(i) ?: continue
                    x += point.x; y += point.y; z += point.z; count++
                }
                if (count == 0) return LandmarkPoint(0.5f, 0.5f)
                val inv = 1f / count
                return LandmarkPoint(x * inv, y * inv, z * inv)
            }

            val leftEye = avg(33, 133, 159, 145, 468)
            val rightEye = avg(362, 263, 386, 374, 473)
            val eyeMid = midpoint(leftEye, rightEye)
            val mouth = avg(13, 14, 61, 291)
            val noseCenter = avg(1, 2, 4)
            val leftCheek = avg(50, 117, 123, 187)
            val rightCheek = avg(280, 346, 352, 411)
            val leftJaw = avg(234, 132, 172, 136)
            val rightJaw = avg(454, 361, 397, 365)
            val noseLeft = avg(98, 97, 64)
            val noseRight = avg(326, 327, 294)
            val forehead = p(10)
            val chin = p(152)
            val faceCenter = LandmarkPoint(
                x = eyeMid.x * 0.38f + noseCenter.x * 0.32f + mouth.x * 0.30f,
                y = eyeMid.y * 0.38f + noseCenter.y * 0.32f + mouth.y * 0.30f,
            )

            val axisX = normalize(
                LandmarkPoint(rightEye.x - leftEye.x, rightEye.y - leftEye.y),
                LandmarkPoint(1f, 0f),
            )
            val axisY = LandmarkPoint(-axisX.y, axisX.x)
            val width = distance(leftJaw, rightJaw).coerceAtLeast(0.10f)
            val height = distance(forehead, chin).coerceAtLeast(0.14f)

            val yawNorm = (yawDegrees / 55f).coerceIn(-1f, 1f)
            val pitchNorm = abs(pitchDegrees / 42f).coerceIn(0f, 1f)
            val poseWeight = ((1f - abs(yawNorm) * 0.78f) * (1f - pitchNorm * 0.45f))
                .coerceIn(0.12f, 1f)

            // Derive side visibility from the actual mesh rather than screen-left/right. This
            // remains correct after mirroring and naturally weakens the compressed/occluded side.
            val leftSpan = distance(noseCenter, leftJaw)
            val rightSpan = distance(noseCenter, rightJaw)
            val meanSpan = ((leftSpan + rightSpan) * 0.5f).coerceAtLeast(1e-4f)
            val leftVisibility = (leftSpan / meanSpan).coerceIn(0.34f, 1f)
            val rightVisibility = (rightSpan / meanSpan).coerceIn(0.34f, 1f)

            return FaceEffectGeometry(
                leftEye = leftEye,
                rightEye = rightEye,
                leftCheek = leftCheek,
                rightCheek = rightCheek,
                leftJaw = leftJaw,
                rightJaw = rightJaw,
                noseCenter = noseCenter,
                noseLeft = noseLeft,
                noseRight = noseRight,
                mouthCenter = mouth,
                faceCenter = faceCenter,
                faceAxisX = axisX,
                faceAxisY = axisY,
                faceWidth = width,
                faceHeight = height,
                leftVisibility = leftVisibility,
                rightVisibility = rightVisibility,
                poseWeight = poseWeight,
            )
        }

        private fun midpoint(a: LandmarkPoint, b: LandmarkPoint) = LandmarkPoint(
            (a.x + b.x) * 0.5f,
            (a.y + b.y) * 0.5f,
            (a.z + b.z) * 0.5f,
        )

        private fun normalize(v: LandmarkPoint, fallback: LandmarkPoint): LandmarkPoint {
            val len = hypot(v.x.toDouble(), v.y.toDouble()).toFloat()
            return if (len < 1e-5f) fallback else LandmarkPoint(v.x / len, v.y / len)
        }

        private fun distance(a: LandmarkPoint, b: LandmarkPoint): Float =
            hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()
    }
}
