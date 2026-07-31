package com.beautymirror.app.rendering

import com.beautymirror.app.tracking.LandmarkPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FaceEffectGeometryTest {
    @Test
    fun base468PointMeshDoesNotRequireIrisLandmarks() {
        val points = MutableList(468) { LandmarkPoint(0.5f, 0.5f, 0f) }
        points[10] = LandmarkPoint(0.50f, 0.16f)
        points[152] = LandmarkPoint(0.50f, 0.88f)
        points[33] = LandmarkPoint(0.31f, 0.40f)
        points[133] = LandmarkPoint(0.41f, 0.40f)
        points[362] = LandmarkPoint(0.59f, 0.40f)
        points[263] = LandmarkPoint(0.69f, 0.40f)
        points[1] = LandmarkPoint(0.50f, 0.53f)
        points[2] = LandmarkPoint(0.50f, 0.54f)
        points[4] = LandmarkPoint(0.50f, 0.55f)
        points[13] = LandmarkPoint(0.49f, 0.68f)
        points[14] = LandmarkPoint(0.51f, 0.68f)
        points[61] = LandmarkPoint(0.40f, 0.68f)
        points[291] = LandmarkPoint(0.60f, 0.68f)
        points[234] = LandmarkPoint(0.20f, 0.55f)
        points[454] = LandmarkPoint(0.80f, 0.55f)

        val geometry = FaceEffectGeometry.from(points, yawDegrees = 0f, pitchDegrees = 0f)

        assertThat(geometry).isNotNull()
        assertThat(geometry!!.faceWidth).isGreaterThan(0.1f)
        assertThat(geometry.faceHeight).isGreaterThan(0.1f)
        assertThat(geometry.poseWeight).isWithin(0.001f).of(1f)
    }

    @Test
    fun steepPoseAttenuatesButDoesNotDisableEffects() {
        val points = MutableList(468) { LandmarkPoint(0.5f, 0.5f, 0f) }
        points[10] = LandmarkPoint(0.50f, 0.16f)
        points[152] = LandmarkPoint(0.50f, 0.88f)
        points[33] = LandmarkPoint(0.31f, 0.40f)
        points[133] = LandmarkPoint(0.41f, 0.40f)
        points[362] = LandmarkPoint(0.59f, 0.40f)
        points[263] = LandmarkPoint(0.69f, 0.40f)
        points[234] = LandmarkPoint(0.20f, 0.55f)
        points[454] = LandmarkPoint(0.80f, 0.55f)

        val frontal = FaceEffectGeometry.from(points, 0f, 0f)!!
        val turned = FaceEffectGeometry.from(points, 55f, 38f)!!

        assertThat(turned.poseWeight).isLessThan(frontal.poseWeight)
        assertThat(turned.poseWeight).isAtLeast(0.12f)
    }
}
