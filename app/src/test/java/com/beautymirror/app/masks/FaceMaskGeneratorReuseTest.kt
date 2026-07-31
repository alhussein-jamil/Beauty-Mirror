package com.beautymirror.app.masks

import com.beautymirror.app.tracking.FaceBounds
import com.beautymirror.app.tracking.FaceTrackingResult
import com.beautymirror.app.tracking.LandmarkPoint
import com.beautymirror.app.tracking.TrackingState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FaceMaskGeneratorReuseTest {
    private fun tracking(): FaceTrackingResult {
        val tex = MutableList(478) { LandmarkPoint(0.5f, 0.5f) }
        FaceMaskGeometry.FACE_OVAL.forEachIndexed { i, idx ->
            tex[idx] = LandmarkPoint(0.25f + (i % 12) * 0.04f, 0.35f + (i % 5) * 0.05f)
        }
        FaceMaskGeometry.LEFT_EYE.forEachIndexed { i, idx ->
            tex[idx] = LandmarkPoint(0.35f + i * 0.01f, 0.45f)
        }
        FaceMaskGeometry.RIGHT_EYE.forEachIndexed { i, idx ->
            tex[idx] = LandmarkPoint(0.55f + i * 0.01f, 0.45f)
        }
        return FaceTrackingResult(
            timestampNs = 0L,
            bounds = FaceBounds(0.2f, 0.2f, 0.8f, 0.9f),
            normalizedLandmarks = tex,
            displayLandmarks = tex,
            textureLandmarks = tex,
            trackingConfidence = 1f,
            presenceConfidence = 1f,
            isValid = true,
            timeSinceValidMs = 0L,
            state = TrackingState.TRACKING,
            effectOpacity = 1f,
        )
    }

    @Test
    fun generateTwiceReusesPolygonLists() {
        val gen = FaceMaskGenerator()
        val a = gen.generate(tracking())
        val b = gen.generate(tracking())
        assertThat(a).isNotNull()
        assertThat(b).isNotNull()
        // Same scratch instances (same-GL-thread reuse contract).
        assertThat(a!!.skin === b!!.skin).isTrue()
        assertThat(a.leftUnderEye === b.leftUnderEye).isTrue()
    }
}
