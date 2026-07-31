package com.beautymirror.app.masks

import com.beautymirror.app.tracking.FaceBounds
import com.beautymirror.app.tracking.FaceTrackingResult
import com.beautymirror.app.tracking.LandmarkPoint
import com.beautymirror.app.tracking.TrackingState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FaceMaskGeneratorTest {
    private fun tracking(texture: List<LandmarkPoint>, display: List<LandmarkPoint> = emptyList()) =
        FaceTrackingResult(
            timestampNs = 0L,
            bounds = FaceBounds(0.2f, 0.2f, 0.8f, 0.9f),
            normalizedLandmarks = texture,
            displayLandmarks = display,
            textureLandmarks = texture,
            trackingConfidence = 1f,
            presenceConfidence = 1f,
            isValid = true,
            timeSinceValidMs = 0L,
            state = TrackingState.TRACKING,
            effectOpacity = 1f,
        )

    @Test
    fun usesTextureLandmarksOnly() {
        val tex = MutableList(478) { LandmarkPoint(0.5f, 0.5f) }
        FaceMaskGeometry.FACE_OVAL.forEachIndexed { i, idx ->
            tex[idx] = LandmarkPoint(0.3f + i * 0.01f, 0.4f)
        }
        FaceMaskGeometry.LEFT_IRIS.forEachIndexed { i, idx ->
            tex[idx] = LandmarkPoint(0.35f + i * 0.01f, 0.42f)
        }
        FaceMaskGeometry.RIGHT_IRIS.forEachIndexed { i, idx ->
            tex[idx] = LandmarkPoint(0.65f + i * 0.01f, 0.42f)
        }
        val gen = FaceMaskGenerator()
        val poly = gen.generate(tracking(tex))
        assertThat(poly).isNotNull()
        assertThat(poly!!.skin.size).isEqualTo(FaceMaskGeometry.FACE_OVAL.size)
        assertThat(poly.leftIris).hasSize(FaceMaskGeometry.LEFT_IRIS.size)
        assertThat(poly.rightIris).hasSize(FaceMaskGeometry.RIGHT_IRIS.size)
    }

    @Test
    fun rejectsWhenOnlyDisplayLandmarksPresent() {
        val display = MutableList(478) { LandmarkPoint(0.1f, 0.1f) }
        val emptyTex = emptyList<LandmarkPoint>()
        val result = FaceTrackingResult(
            timestampNs = 0L,
            bounds = FaceBounds(0f, 0f, 1f, 1f),
            normalizedLandmarks = display,
            displayLandmarks = display,
            textureLandmarks = emptyTex,
            trackingConfidence = 1f,
            presenceConfidence = 1f,
            isValid = true,
            timeSinceValidMs = 0L,
            state = TrackingState.TRACKING,
            effectOpacity = 1f,
        )
        assertThat(FaceMaskGenerator().generate(result)).isNull()
    }
}
