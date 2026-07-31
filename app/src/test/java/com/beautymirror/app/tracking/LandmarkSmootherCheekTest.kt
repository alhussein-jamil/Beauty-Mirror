package com.beautymirror.app.tracking

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LandmarkSmootherCheekTest {
    private fun raw(luma: Float, leftUv: LandmarkPoint, rightUv: LandmarkPoint) = FaceTrackingResult(
        timestampNs = 1L,
        bounds = FaceBounds(0.2f, 0.2f, 0.8f, 0.8f),
        normalizedLandmarks = listOf(LandmarkPoint(0.5f, 0.5f), LandmarkPoint(0.4f, 0.4f)),
        displayLandmarks = emptyList(),
        textureLandmarks = emptyList(),
        trackingConfidence = 1f,
        presenceConfidence = 1f,
        isValid = true,
        timeSinceValidMs = 0L,
        state = TrackingState.TRACKING,
        effectOpacity = 1f,
        faceLuminance = luma,
        leftCheekUv = leftUv,
        rightCheekUv = rightUv,
    )

    @Test
    fun cheekUvSurvivesHold() {
        val s = LandmarkSmoother(holdTimeoutMs = 500L, fadeDurationMs = 300L)
        s.push(0L, raw(0.5f, LandmarkPoint(0.33f, 0.44f), LandmarkPoint(0.66f, 0.44f)))
        val held = s.push(100L, null)
        assertThat(held.state).isEqualTo(TrackingState.HOLDING)
        assertThat(held.leftCheekUv.x).isWithin(0.05f).of(0.33f)
        assertThat(held.rightCheekUv.x).isWithin(0.05f).of(0.66f)
    }
}
