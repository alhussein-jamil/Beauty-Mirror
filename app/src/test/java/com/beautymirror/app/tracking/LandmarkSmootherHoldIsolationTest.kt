package com.beautymirror.app.tracking

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LandmarkSmootherHoldIsolationTest {
    private fun face(xs: FloatArray): FaceTrackingResult {
        val pts = xs.map { LandmarkPoint(it, 0.5f) }
        return FaceTrackingResult(
            timestampNs = 0L,
            bounds = FaceBounds(0.2f, 0.2f, 0.8f, 0.8f),
            normalizedLandmarks = pts,
            displayLandmarks = pts,
            textureLandmarks = pts,
            trackingConfidence = 1f,
            presenceConfidence = 1f,
            isValid = true,
            timeSinceValidMs = 0L,
            state = TrackingState.TRACKING,
            effectOpacity = 1f,
        )
    }

    @Test
    fun holdLandmarksSurviveNextPushMutation() {
        val s = LandmarkSmoother()
        val first = s.push(1_000L, face(floatArrayOf(0.1f, 0.2f, 0.3f)))
        val held = s.push(1_050L, null)
        assertThat(held.state).isEqualTo(TrackingState.HOLDING)
        val heldX = held.normalizedLandmarks.map { it.x }
        // New face with different coords should not mutate the held snapshot.
        s.push(1_100L, face(floatArrayOf(0.9f, 0.8f, 0.7f)))
        assertThat(held.normalizedLandmarks.map { it.x }).isEqualTo(heldX)
        assertThat(first.normalizedLandmarks).isNotEmpty()
    }
}
