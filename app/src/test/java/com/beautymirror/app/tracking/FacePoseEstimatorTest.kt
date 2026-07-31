package com.beautymirror.app.tracking

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FacePoseEstimatorTest {
    private fun frontal(): MutableList<LandmarkPoint> {
        val points = MutableList(478) { LandmarkPoint(0.5f, 0.5f, 0f) }
        points[33] = LandmarkPoint(0.30f, 0.40f)
        points[133] = LandmarkPoint(0.40f, 0.40f)
        points[362] = LandmarkPoint(0.60f, 0.40f)
        points[263] = LandmarkPoint(0.70f, 0.40f)
        points[1] = LandmarkPoint(0.50f, 0.53f, -0.03f)
        points[234] = LandmarkPoint(0.20f, 0.54f, 0f)
        points[454] = LandmarkPoint(0.80f, 0.54f, 0f)
        points[13] = LandmarkPoint(0.48f, 0.68f)
        points[14] = LandmarkPoint(0.52f, 0.68f)
        points[10] = LandmarkPoint(0.50f, 0.18f)
        points[152] = LandmarkPoint(0.50f, 0.88f)
        return points
    }

    @Test
    fun symmetricFaceHasNearZeroYawAndRoll() {
        val pose = FacePoseEstimator.estimate(frontal())
        assertThat(pose.yawDegrees).isWithin(3f).of(0f)
        assertThat(pose.rollDegrees).isWithin(1f).of(0f)
    }

    @Test
    fun eyeSlopeProducesRoll() {
        val points = frontal()
        points[362] = points[362].copy(y = 0.46f)
        points[263] = points[263].copy(y = 0.46f)
        assertThat(FacePoseEstimator.estimate(points).rollDegrees).isGreaterThan(5f)
    }

    @Test
    fun incompleteMeshReturnsNeutralPose() {
        assertThat(FacePoseEstimator.estimate(emptyList())).isEqualTo(
            FacePoseEstimator.Pose(0f, 0f, 0f),
        )
    }

    @Test
    fun canonicalTransformIdentityProducesNeutralPose() {
        val pose = FacePoseEstimator.estimate(frontal(), identityMatrix())
        assertThat(pose.yawDegrees).isWithin(0.01f).of(0f)
        assertThat(pose.pitchDegrees).isWithin(0.01f).of(0f)
        assertThat(pose.rollDegrees).isWithin(0.01f).of(0f)
    }

    @Test
    fun canonicalTransformExtractsYaw() {
        val matrix = FacePoseEstimator.fromTransformationMatrix(yawMatrix(30f))!!
        assertThat(matrix.yawDegrees).isWithin(0.1f).of(30f)
        assertThat(matrix.pitchDegrees).isWithin(0.1f).of(0f)
        assertThat(matrix.rollDegrees).isWithin(0.1f).of(0f)
        // estimate() blends matrix with near-neutral landmarks for stability.
        val blended = FacePoseEstimator.estimate(frontal(), yawMatrix(30f))
        assertThat(blended.yawDegrees).isWithin(0.2f).of(21f)
    }

    @Test
    fun canonicalTransformExtractsPitch() {
        val matrix = FacePoseEstimator.fromTransformationMatrix(pitchMatrix(20f))!!
        assertThat(matrix.pitchDegrees).isWithin(0.1f).of(20f)
        assertThat(matrix.yawDegrees).isWithin(0.1f).of(0f)
        assertThat(matrix.rollDegrees).isWithin(0.1f).of(0f)
        val blended = FacePoseEstimator.estimate(frontal(), pitchMatrix(20f))
        assertThat(blended.pitchDegrees).isWithin(0.2f).of(15f)
    }

    @Test
    fun canonicalTransformExtractsRoll() {
        val matrix = FacePoseEstimator.fromTransformationMatrix(rollMatrix(15f))!!
        assertThat(matrix.rollDegrees).isWithin(0.1f).of(15f)
        assertThat(matrix.yawDegrees).isWithin(0.1f).of(0f)
        assertThat(matrix.pitchDegrees).isWithin(0.1f).of(0f)
        val blended = FacePoseEstimator.estimate(frontal(), rollMatrix(15f))
        assertThat(blended.rollDegrees).isWithin(0.2f).of(9.75f)
    }

    @Test
    fun invalidCanonicalTransformFallsBackToLandmarks() {
        val invalid = FloatArray(16)
        val fallback = FacePoseEstimator.estimate(frontal())
        assertThat(FacePoseEstimator.estimate(frontal(), invalid)).isEqualTo(fallback)
    }

    private fun identityMatrix() = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f,
    )

    private fun yawMatrix(degrees: Float): FloatArray {
        val radians = Math.toRadians(degrees.toDouble())
        val c = kotlin.math.cos(radians).toFloat()
        val s = kotlin.math.sin(radians).toFloat()
        return floatArrayOf(
            c, 0f, -s, 0f,
            0f, 1f, 0f, 0f,
            s, 0f, c, 0f,
            0f, 0f, 0f, 1f,
        )
    }

    private fun pitchMatrix(degrees: Float): FloatArray {
        val radians = Math.toRadians(degrees.toDouble())
        val c = kotlin.math.cos(radians).toFloat()
        val s = kotlin.math.sin(radians).toFloat()
        return floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, c, s, 0f,
            0f, -s, c, 0f,
            0f, 0f, 0f, 1f,
        )
    }

    private fun rollMatrix(degrees: Float): FloatArray {
        val radians = Math.toRadians(degrees.toDouble())
        val c = kotlin.math.cos(radians).toFloat()
        val s = kotlin.math.sin(radians).toFloat()
        return floatArrayOf(
            c, s, 0f, 0f,
            -s, c, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f,
        )
    }
}
