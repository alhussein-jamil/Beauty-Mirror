package com.beautymirror.app.masks

import com.beautymirror.app.tracking.LandmarkPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FeatureExclusionMaskGeneratorTest {
    private fun poly(vararg xy: Float): List<LandmarkPoint> {
        val out = ArrayList<LandmarkPoint>(xy.size / 2)
        var i = 0
        while (i < xy.size) {
            out += LandmarkPoint(xy[i], xy[i + 1])
            i += 2
        }
        return out
    }

    @Test
    fun includesHairlineWhenOvalPresent() {
        val oval = (0 until 12).map { i ->
            LandmarkPoint(0.2f + i * 0.05f, 0.7f - (i % 3) * 0.05f)
        }
        val masks = MaskPolygons(
            faceOval = oval,
            skin = oval,
            leftEye = poly(0.3f, 0.5f, 0.32f, 0.52f, 0.34f, 0.5f),
            rightEye = poly(0.6f, 0.5f, 0.62f, 0.52f, 0.64f, 0.5f),
            leftBrow = emptyList(),
            rightBrow = emptyList(),
            lips = emptyList(),
            mouthInterior = emptyList(),
            nostrils = emptyList(),
            leftUnderEye = emptyList(),
            rightUnderEye = emptyList(),
        )
        val ex = FeatureExclusionMaskGenerator().exclusions(masks)
        assertThat(ex.size).isAtLeast(3) // eyes + hairline
        assertThat(ex.last().size).isAtLeast(3)
    }

    @Test
    fun expandsEyeExclusionsSlightly() {
        val eye = poly(0.3f, 0.5f, 0.35f, 0.5f, 0.325f, 0.55f)
        val masks = MaskPolygons(
            faceOval = emptyList(),
            skin = emptyList(),
            leftEye = eye,
            rightEye = emptyList(),
            leftBrow = emptyList(),
            rightBrow = emptyList(),
            lips = emptyList(),
            mouthInterior = emptyList(),
            nostrils = emptyList(),
            leftUnderEye = emptyList(),
            rightUnderEye = emptyList(),
        )
        val ex = FeatureExclusionMaskGenerator().exclusions(masks)
        assertThat(ex).hasSize(1)
        // Expanded polygon should not be identical to original.
        assertThat(ex[0][0].x).isNotEqualTo(eye[0].x)
    }
}
