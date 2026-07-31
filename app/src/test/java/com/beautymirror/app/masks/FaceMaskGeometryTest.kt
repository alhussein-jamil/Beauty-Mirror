package com.beautymirror.app.masks

import com.google.common.truth.Truth.assertThat
import com.beautymirror.app.tracking.LandmarkPoint
import org.junit.Test

class FaceMaskGeometryTest {
    private fun fakeLandmarks(): List<LandmarkPoint> {
        val pts = MutableList(478) { LandmarkPoint(0.5f, 0.5f) }
        val leftLower = intArrayOf(33, 7, 163, 144, 145, 153, 154, 155, 133)
        leftLower.forEachIndexed { i, idx ->
            pts[idx] = LandmarkPoint(0.3f + i * 0.02f, 0.40f)
        }
        val rightLower = intArrayOf(263, 249, 390, 373, 374, 380, 381, 382, 362)
        rightLower.forEachIndexed { i, idx ->
            pts[idx] = LandmarkPoint(0.6f + i * 0.02f, 0.40f)
        }
        pts[10] = LandmarkPoint(0.2f, 0.2f)
        pts[152] = LandmarkPoint(0.8f, 0.9f)
        return pts
    }

    @Test
    fun underEyeExtendsDownInDisplaySpace() {
        val lm = fakeLandmarks()
        val poly = FaceMaskGeometry.underEyePolygon(lm, left = true, yIncreasesDownward = true)
        assertThat(poly.size).isAtLeast(6)
        val lidY = poly.take(9).map { it.y }.average()
        val cheekY = poly.drop(9).map { it.y }.average()
        assertThat(cheekY).isGreaterThan(lidY)
    }

    @Test
    fun underEyeExtendsTowardLowerYInTextureSpace() {
        val pts = MutableList(478) { LandmarkPoint(0.5f, 0.5f) }
        val leftLower = intArrayOf(33, 7, 163, 144, 145, 153, 154, 155, 133)
        leftLower.forEachIndexed { i, idx ->
            pts[idx] = LandmarkPoint(0.3f + i * 0.02f, 0.60f)
        }
        pts[10] = LandmarkPoint(0.2f, 0.8f)
        pts[152] = LandmarkPoint(0.8f, 0.1f)
        val poly = FaceMaskGeometry.underEyePolygon(pts, left = true, yIncreasesDownward = false)
        val lidY = poly.take(9).map { it.y }.average()
        val cheekY = poly.drop(9).map { it.y }.average()
        assertThat(cheekY).isLessThan(lidY)
    }

    @Test
    fun safePointsSkipsMissing() {
        val pts = listOf(LandmarkPoint(0f, 0f), LandmarkPoint(1f, 1f))
        val out = FaceMaskGeometry.safePoints(pts, intArrayOf(0, 5, 1))
        assertThat(out).hasSize(2)
    }

    @Test
    fun expandPolygonMovesOutward() {
        val poly = listOf(
            LandmarkPoint(0.4f, 0.4f),
            LandmarkPoint(0.6f, 0.4f),
            LandmarkPoint(0.5f, 0.6f),
        )
        val expanded = FaceMaskGeometry.expandPolygon(poly, 0.05f)
        assertThat(expanded[0].x).isLessThan(poly[0].x)
        assertThat(expanded[1].x).isGreaterThan(poly[1].x)
        assertThat(expanded[2].y).isGreaterThan(poly[2].y)
    }

    @Test
    fun expandPolygonIntoMatchesExpandPolygon() {
        val poly = listOf(
            LandmarkPoint(0.4f, 0.4f),
            LandmarkPoint(0.6f, 0.4f),
            LandmarkPoint(0.5f, 0.6f),
        )
        val a = FaceMaskGeometry.expandPolygon(poly, 0.05f)
        val b = ArrayList<LandmarkPoint>()
        FaceMaskGeometry.expandPolygonInto(poly, 0.05f, b)
        assertThat(b).hasSize(a.size)
        for (i in a.indices) {
            assertThat(b[i].x).isWithin(1e-5f).of(a[i].x)
            assertThat(b[i].y).isWithin(1e-5f).of(a[i].y)
        }
    }

    @Test
    fun underEyeUsesProvidedFaceWidth() {
        val lm = fakeLandmarks()
        val wide = FaceMaskGeometry.underEyePolygon(lm, left = true, faceWidthNorm = 0.9f)
        val narrow = FaceMaskGeometry.underEyePolygon(lm, left = true, faceWidthNorm = 0.15f)
        val wideDrop = wide.drop(9).map { it.y }.average() - wide.take(9).map { it.y }.average()
        val narrowDrop = narrow.drop(9).map { it.y }.average() - narrow.take(9).map { it.y }.average()
        assertThat(wideDrop).isGreaterThan(narrowDrop)
    }

    @Test
    fun underEyePolygonIntoMatchesAllocatingApi() {
        val lm = fakeLandmarks()
        val a = FaceMaskGeometry.underEyePolygon(lm, left = true, yIncreasesDownward = true, faceWidthNorm = 0.4f)
        val b = ArrayList<com.beautymirror.app.tracking.LandmarkPoint>()
        FaceMaskGeometry.underEyePolygonInto(lm, left = true, yIncreasesDownward = true, faceWidthNorm = 0.4f, dest = b)
        assertThat(b).hasSize(a.size)
        for (i in a.indices) {
            assertThat(b[i].x).isWithin(1e-5f).of(a[i].x)
            assertThat(b[i].y).isWithin(1e-5f).of(a[i].y)
        }
    }
}
