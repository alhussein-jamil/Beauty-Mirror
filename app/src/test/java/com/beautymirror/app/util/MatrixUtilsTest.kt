package com.beautymirror.app.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MatrixUtilsTest {
    @Test
    fun mirrorFrontFlipsX() {
        val (x, y) = MatrixUtils.mapNormalizedPoint(0.2f, 0.4f, 0, mirrorX = true)
        assertThat(x).isWithin(1e-4f).of(0.8f)
        assertThat(y).isWithin(1e-4f).of(0.4f)
    }

    @Test
    fun rotation90() {
        val (x, y) = MatrixUtils.mapNormalizedPoint(0.0f, 0.0f, 90, mirrorX = false)
        assertThat(x).isWithin(1e-4f).of(1.0f)
        assertThat(y).isWithin(1e-4f).of(0.0f)
    }

    @Test
    fun rotation180() {
        val (x, y) = MatrixUtils.mapNormalizedPoint(0.25f, 0.25f, 180, mirrorX = false)
        assertThat(x).isWithin(1e-4f).of(0.75f)
        assertThat(y).isWithin(1e-4f).of(0.75f)
    }

    @Test
    fun rotation270() {
        val (x, y) = MatrixUtils.mapNormalizedPoint(0.0f, 0.0f, 270, mirrorX = false)
        assertThat(x).isWithin(1e-4f).of(0.0f)
        assertThat(y).isWithin(1e-4f).of(1.0f)
    }

    @Test
    fun rotation90ThenInverseReturnsOrigin() {
        val (ux, uy) = MatrixUtils.mapNormalizedPoint(0.2f, 0.3f, 90, mirrorX = false)
        // Inverse of 90 CW is 270 CW.
        val (sx, sy) = MatrixUtils.mapNormalizedPoint(ux, uy, 270, mirrorX = false)
        assertThat(sx).isWithin(1e-4f).of(0.2f)
        assertThat(sy).isWithin(1e-4f).of(0.3f)
    }

    @Test
    fun uprightNormalizedToSensorInvertsMapNormalized() {
        for (rot in listOf(0, 90, 180, 270)) {
            val (ux, uy) = MatrixUtils.mapNormalizedPoint(0.2f, 0.3f, rot, mirrorX = false)
            val (sx, sy) = MatrixUtils.uprightNormalizedToSensor(ux, uy, rot)
            assertThat(sx).isWithin(1e-4f).of(0.2f)
            assertThat(sy).isWithin(1e-4f).of(0.3f)
        }
    }

    @Test
    fun cropScaleToFitLetterboxesWideSource() {
        val m = MatrixUtils.cropScaleToFit(16f, 9f, 9f, 16f)
        // Wide source into tall dest → horizontal scale < 1
        assertThat(m[0]).isLessThan(1f)
        assertThat(m[5]).isWithin(1e-4f).of(1f)
    }

    @Test
    fun multiplyIdentityLeavesMatrix() {
        val id = FloatArray(16)
        MatrixUtils.setIdentity(id)
        val a = floatArrayOf(
            1f, 2f, 3f, 4f,
            5f, 6f, 7f, 8f,
            9f, 10f, 11f, 12f,
            13f, 14f, 15f, 16f,
        )
        val out = FloatArray(16)
        MatrixUtils.multiply(out, a, id)
        for (i in 0 until 16) {
            assertThat(out[i]).isWithin(1e-4f).of(a[i])
        }
        // In-place safe when result aliases lhs.
        MatrixUtils.multiply(a, a, id)
        for (i in 0 until 16) {
            assertThat(a[i]).isWithin(1e-4f).of(out[i])
        }
    }
}
