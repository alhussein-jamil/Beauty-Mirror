package com.beautymirror.app.masks

import com.beautymirror.app.tracking.FaceTrackingResult
import com.beautymirror.app.tracking.LandmarkPoint

data class MaskPolygons(
    val faceOval: List<LandmarkPoint>,
    val skin: List<LandmarkPoint>,
    val leftEye: List<LandmarkPoint>,
    val rightEye: List<LandmarkPoint>,
    val leftIris: List<LandmarkPoint> = emptyList(),
    val rightIris: List<LandmarkPoint> = emptyList(),
    val leftBrow: List<LandmarkPoint>,
    val rightBrow: List<LandmarkPoint>,
    val lips: List<LandmarkPoint>,
    val mouthInterior: List<LandmarkPoint>,
    val nostrils: List<LandmarkPoint>,
    val leftUnderEye: List<LandmarkPoint>,
    val rightUnderEye: List<LandmarkPoint>,
)

/**
 * Builds mask polygons from texture-space landmarks.
 * Reuses scratch lists across frames to cut GC. Contract: caller must finish consuming
 * polygons (mask render) before the next generate() call — same GL thread in RenderGraph.
 */
class FaceMaskGenerator {
    private val ovalScratch = ArrayList<LandmarkPoint>(40)
    private val skinScratch = ArrayList<LandmarkPoint>(40)
    private val leftEyeScratch = ArrayList<LandmarkPoint>(20)
    private val rightEyeScratch = ArrayList<LandmarkPoint>(20)
    private val leftIrisScratch = ArrayList<LandmarkPoint>(8)
    private val rightIrisScratch = ArrayList<LandmarkPoint>(8)
    private val leftBrowScratch = ArrayList<LandmarkPoint>(12)
    private val rightBrowScratch = ArrayList<LandmarkPoint>(12)
    private val lipsScratch = ArrayList<LandmarkPoint>(24)
    private val mouthScratch = ArrayList<LandmarkPoint>(16)
    private val nostrilScratch = ArrayList<LandmarkPoint>(8)
    private val leftUnderEyeScratch = ArrayList<LandmarkPoint>(24)
    private val rightUnderEyeScratch = ArrayList<LandmarkPoint>(24)

    fun generate(tracking: FaceTrackingResult): MaskPolygons? {
        // Texture landmarks only — they share the camera pass mirror and GL Y-up convention.
        val lm = tracking.textureLandmarks
        if (lm.size < 400) return null
        if (!tracking.isValid && tracking.effectOpacity <= 0.01f) return null

        fill(ovalScratch, lm, FaceMaskGeometry.FACE_OVAL)
        FaceMaskGeometry.expandPolygonInto(ovalScratch, amount = 0.015f, dest = skinScratch)
        fill(leftEyeScratch, lm, FaceMaskGeometry.LEFT_EYE)
        fill(rightEyeScratch, lm, FaceMaskGeometry.RIGHT_EYE)
        fill(leftIrisScratch, lm, FaceMaskGeometry.LEFT_IRIS)
        fill(rightIrisScratch, lm, FaceMaskGeometry.RIGHT_IRIS)
        fill(leftBrowScratch, lm, FaceMaskGeometry.LEFT_EYEBROW)
        fill(rightBrowScratch, lm, FaceMaskGeometry.RIGHT_EYEBROW)
        fill(lipsScratch, lm, FaceMaskGeometry.LIPS_OUTER)
        fill(mouthScratch, lm, FaceMaskGeometry.LIPS_INNER)
        fill(nostrilScratch, lm, FaceMaskGeometry.NOSTRILS)

        val faceW = tracking.bounds.width.takeIf { it > 0.05f } ?: run {
            var minX = 1f
            var maxX = 0f
            for (p in lm) {
                if (p.x < minX) minX = p.x
                if (p.x > maxX) maxX = p.x
            }
            (maxX - minX).coerceAtLeast(0.15f)
        }

        return MaskPolygons(
            faceOval = ovalScratch,
            skin = skinScratch,
            leftEye = leftEyeScratch,
            rightEye = rightEyeScratch,
            leftIris = leftIrisScratch,
            rightIris = rightIrisScratch,
            leftBrow = leftBrowScratch,
            rightBrow = rightBrowScratch,
            lips = lipsScratch,
            mouthInterior = mouthScratch,
            nostrils = nostrilScratch,
            leftUnderEye = leftUnderEyeScratch.also {
                FaceMaskGeometry.underEyePolygonInto(
                    lm, left = true, yIncreasesDownward = false,
                    faceWidthNorm = faceW, dest = it,
                )
            },
            rightUnderEye = rightUnderEyeScratch.also {
                FaceMaskGeometry.underEyePolygonInto(
                    lm, left = false, yIncreasesDownward = false,
                    faceWidthNorm = faceW, dest = it,
                )
            },
        )
    }

    private fun fill(dest: ArrayList<LandmarkPoint>, landmarks: List<LandmarkPoint>, indices: IntArray) {
        dest.clear()
        for (i in indices) {
            landmarks.getOrNull(i)?.let { dest.add(it) }
        }
    }
}
