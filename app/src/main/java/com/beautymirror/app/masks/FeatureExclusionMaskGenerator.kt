package com.beautymirror.app.masks

import com.beautymirror.app.tracking.LandmarkPoint

/**
 * Collects feature polygons that must be carved out of the skin mask.
 * Reuses scratch lists — same-GL-thread contract as [FaceMaskGenerator].
 */
class FeatureExclusionMaskGenerator {
    private val out = ArrayList<List<LandmarkPoint>>(12)
    private val leftEyeExp = ArrayList<LandmarkPoint>(20)
    private val rightEyeExp = ArrayList<LandmarkPoint>(20)
    private val nostrilExp = ArrayList<LandmarkPoint>(8)
    private val hairline = ArrayList<LandmarkPoint>(40)
    private val topBand = ArrayList<LandmarkPoint>(20)
    private val topLifted = ArrayList<LandmarkPoint>(20)

    fun exclusions(polygons: MaskPolygons): List<List<LandmarkPoint>> {
        out.clear()
        FaceMaskGeometry.expandPolygonInto(polygons.leftEye, 0.008f, leftEyeExp)
        if (leftEyeExp.size >= 3) out.add(leftEyeExp)
        FaceMaskGeometry.expandPolygonInto(polygons.rightEye, 0.008f, rightEyeExp)
        if (rightEyeExp.size >= 3) out.add(rightEyeExp)
        if (polygons.leftBrow.size >= 3) out.add(polygons.leftBrow)
        if (polygons.rightBrow.size >= 3) out.add(polygons.rightBrow)
        if (polygons.lips.size >= 3) out.add(polygons.lips)
        if (polygons.mouthInterior.size >= 3) out.add(polygons.mouthInterior)
        if (polygons.nostrils.size >= 3) {
            FaceMaskGeometry.expandPolygonInto(polygons.nostrils, 0.006f, nostrilExp)
            if (nostrilExp.size >= 3) out.add(nostrilExp)
        }
        hairlineBandInto(polygons.faceOval, hairline)
        if (hairline.size >= 3) out.add(hairline)
        return out
    }

    /**
     * Soft hairline strip from the upper face-oval arc, extended slightly upward.
     * Approximate — facial-hair / bangs remain a known limitation.
     */
    private fun hairlineBandInto(faceOval: List<LandmarkPoint>, dest: MutableList<LandmarkPoint>) {
        dest.clear()
        if (faceOval.size < 8) return
        var maxY = 0f
        for (p in faceOval) if (p.y > maxY) maxY = p.y
        // Top band ≈ upper third by Y (texture space: higher Y toward forehead after flip).
        val thresh = maxY - 0.14f
        topBand.clear()
        for (p in faceOval) {
            if (p.y >= thresh) topBand.add(p)
        }
        if (topBand.size < 3) {
            // Fallback: take highest N points without full sort.
            topBand.clear()
            val n = (faceOval.size / 3).coerceAtLeast(4)
            for (p in faceOval) {
                insertTopByY(topBand, p, n)
            }
        }
        if (topBand.size < 3) return
        val lift = 0.045f
        topLifted.clear()
        for (p in topBand) {
            topLifted.add(LandmarkPoint(p.x, (p.y + lift).coerceAtMost(1f)))
        }
        dest.addAll(topBand)
        for (i in topLifted.lastIndex downTo 0) dest.add(topLifted[i])
    }

    /** Keep [cap] highest-Y points in [dest] (insertion, no heap sort). */
    private fun insertTopByY(dest: MutableList<LandmarkPoint>, p: LandmarkPoint, cap: Int) {
        if (dest.size < cap) {
            var i = dest.size
            dest.add(p)
            while (i > 0 && dest[i - 1].y < p.y) {
                dest[i] = dest[i - 1]
                i--
            }
            dest[i] = p
            return
        }
        if (p.y <= dest.last().y) return
        var i = dest.lastIndex
        while (i > 0 && dest[i - 1].y < p.y) {
            dest[i] = dest[i - 1]
            i--
        }
        dest[i] = p
    }
}
