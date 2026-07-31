package com.beautymirror.app.tracking

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Head-pose estimator used to keep beauty effects stable as the face turns.
 *
 * MediaPipe's canonical-face transformation matrix is preferred when it agrees with the landmark
 * estimator. Some devices return matrix axes in a different orientation for portrait rendering,
 * which manifests as ~±90° roll or left/right inverted yaw. In those cases the landmark estimate
 * is used instead, then mirrored into display semantics when the front-camera mirror is enabled.
 */
object FacePoseEstimator {
    data class Pose(
        val yawDegrees: Float,
        val pitchDegrees: Float,
        val rollDegrees: Float,
    )

    fun estimate(
        points: List<LandmarkPoint>,
        facialTransformationMatrix: FloatArray? = null,
        mirrorFront: Boolean = false,
    ): Pose {
        val landmarkPose = fromLandmarks(points)
        val matrixPose = fromTransformationMatrix(facialTransformationMatrix)
        val chosen = choosePose(matrixPose, landmarkPose)
        return if (mirrorFront) {
            chosen.copy(
                yawDegrees = -chosen.yawDegrees,
                rollDegrees = -chosen.rollDegrees,
            )
        } else {
            chosen
        }
    }

    private fun choosePose(matrixPose: Pose?, landmarkPose: Pose): Pose {
        if (matrixPose == null) return landmarkPose
        val rollMismatch = abs(normalizeDegrees(matrixPose.rollDegrees - landmarkPose.rollDegrees))
        val yawMismatch = abs(matrixPose.yawDegrees - landmarkPose.yawDegrees)
        val matrixLooksQuarterTurn = abs(abs(matrixPose.rollDegrees) - 90f) < 22f && abs(landmarkPose.rollDegrees) < 30f
        val matrixLooksMirrored = yawMismatch > 28f && abs(matrixPose.yawDegrees + landmarkPose.yawDegrees) < 18f
        if (matrixLooksQuarterTurn || matrixLooksMirrored || rollMismatch > 34f) return landmarkPose
        return Pose(
            yawDegrees = (matrixPose.yawDegrees * 0.70f + landmarkPose.yawDegrees * 0.30f).coerceIn(-75f, 75f),
            pitchDegrees = (matrixPose.pitchDegrees * 0.75f + landmarkPose.pitchDegrees * 0.25f).coerceIn(-60f, 60f),
            rollDegrees = normalizeDegrees(matrixPose.rollDegrees * 0.65f + landmarkPose.rollDegrees * 0.35f)
                .coerceIn(-90f, 90f),
        )
    }

    /**
     * Converts MediaPipe's flat column-major 4x4 canonical-face transform to Euler angles.
     * Scale is removed from each basis vector before extraction so the result remains stable when
     * the face moves closer to or farther from the camera.
     */
    internal fun fromTransformationMatrix(matrix: FloatArray?): Pose? {
        if (matrix == null || matrix.size < 16 || matrix.any { !it.isFinite() }) return null

        // Column-major basis vectors.
        var r00 = matrix[0]; var r10 = matrix[1]; var r20 = matrix[2]
        var r01 = matrix[4]; var r11 = matrix[5]; var r21 = matrix[6]
        var r02 = matrix[8]; var r12 = matrix[9]; var r22 = matrix[10]

        val xScale = sqrt(r00 * r00 + r10 * r10 + r20 * r20)
        val yScale = sqrt(r01 * r01 + r11 * r11 + r21 * r21)
        val zScale = sqrt(r02 * r02 + r12 * r12 + r22 * r22)
        if (xScale < 1e-4f || yScale < 1e-4f || zScale < 1e-4f) return null

        r00 /= xScale; r10 /= xScale; r20 /= xScale
        r01 /= yScale; r11 /= yScale; r21 /= yScale
        r02 /= zScale; r12 /= zScale; r22 /= zScale

        val determinant =
            r00 * (r11 * r22 - r12 * r21) -
                r01 * (r10 * r22 - r12 * r20) +
                r02 * (r10 * r21 - r11 * r20)
        if (determinant !in 0.45f..1.55f) return null

        val yaw = Math.toDegrees(asin((-r20).coerceIn(-1f, 1f).toDouble())).toFloat()
        val pitch = Math.toDegrees(atan2(r21.toDouble(), r22.toDouble())).toFloat()
        val roll = Math.toDegrees(atan2(r10.toDouble(), r00.toDouble())).toFloat()

        return Pose(
            yawDegrees = yaw.coerceIn(-75f, 75f),
            pitchDegrees = pitch.coerceIn(-60f, 60f),
            rollDegrees = normalizeDegrees(roll).coerceIn(-90f, 90f),
        )
    }

    private fun fromLandmarks(points: List<LandmarkPoint>): Pose {
        if (points.size < 455) return Pose(0f, 0f, 0f)

        val leftEye = midpoint(points[33], points[133])
        val rightEye = midpoint(points[362], points[263])
        val eyeMid = midpoint(leftEye, rightEye)
        val nose = points[1]
        val leftCheek = points[234]
        val rightCheek = points[454]
        val mouth = midpoint(points[13], points[14])
        val forehead = points[10]
        val chin = points[152]

        val roll = Math.toDegrees(
            atan2(
                (rightEye.y - leftEye.y).toDouble(),
                (rightEye.x - leftEye.x).toDouble(),
            ),
        ).toFloat()

        val leftSpan = distance(nose, leftCheek)
        val rightSpan = distance(nose, rightCheek)
        val spanSum = (leftSpan + rightSpan).coerceAtLeast(1e-4f)
        val geometricYaw = ((leftSpan - rightSpan) / spanSum) * 92f
        val cheekDepthAsymmetry = (leftCheek.z - rightCheek.z) * 115f
        val yaw = (geometricYaw * 0.78f + cheekDepthAsymmetry * 0.22f)
            .coerceIn(-60f, 60f)

        val faceHeight = distance(forehead, chin).coerceAtLeast(1e-4f)
        val expectedNoseY = eyeMid.y * 0.56f + mouth.y * 0.44f
        val verticalPitch = ((nose.y - expectedNoseY) / faceHeight) * 125f
        val depthPitch = ((nose.z - (forehead.z + chin.z) * 0.5f) / faceHeight) * 28f
        val pitch = (verticalPitch + depthPitch).coerceIn(-45f, 45f)

        return Pose(yaw, pitch, normalizeDegrees(roll))
    }

    private fun midpoint(a: LandmarkPoint, b: LandmarkPoint) = LandmarkPoint(
        x = (a.x + b.x) * 0.5f,
        y = (a.y + b.y) * 0.5f,
        z = (a.z + b.z) * 0.5f,
    )

    private fun distance(a: LandmarkPoint, b: LandmarkPoint): Float =
        hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()

    private fun normalizeDegrees(value: Float): Float {
        var v = value
        while (v > 180f) v -= 360f
        while (v < -180f) v += 360f
        return v
    }
}
