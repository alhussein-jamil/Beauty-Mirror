package com.beautymirror.app.tracking

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LandmarkSmootherTest {
    private fun result(points: List<LandmarkPoint>) = FaceTrackingResult(
        timestampNs = 0L,
        bounds = FaceBounds(0.2f, 0.2f, 0.8f, 0.8f),
        normalizedLandmarks = points,
        displayLandmarks = points,
        textureLandmarks = points,
        trackingConfidence = 1f,
        presenceConfidence = 1f,
        isValid = true,
        timeSinceValidMs = 0L,
        state = TrackingState.TRACKING,
        effectOpacity = 1f,
    )

    @Test
    fun smoothsTowardNewValue() {
        val smoother = LandmarkSmoother(baseAlpha = 0.5f)
        val a = listOf(LandmarkPoint(0f, 0f))
        val b = listOf(LandmarkPoint(1f, 1f))
        smoother.push(0L, result(a))
        val out = smoother.push(16L, result(b))
        assertThat(out.normalizedLandmarks[0].x).isGreaterThan(0f)
        assertThat(out.normalizedLandmarks[0].x).isLessThan(1f)
        assertThat(out.state).isEqualTo(TrackingState.TRACKING)
    }

    @Test
    fun holdsThenFadesAfterLoss() {
        val smoother = LandmarkSmoother(holdTimeoutMs = 100, fadeDurationMs = 100)
        val pts = listOf(LandmarkPoint(0.4f, 0.4f), LandmarkPoint(0.6f, 0.6f))
        smoother.push(0L, result(pts))
        val held = smoother.push(50L, null)
        assertThat(held.state).isEqualTo(TrackingState.HOLDING)
        assertThat(held.effectOpacity).isGreaterThan(0.5f)
        val fading = smoother.push(150L, null)
        assertThat(fading.state).isEqualTo(TrackingState.FADING)
        val gone = smoother.push(400L, null)
        assertThat(gone.state).isEqualTo(TrackingState.NO_FACE)
        assertThat(gone.effectOpacity).isEqualTo(0f)
    }
}
