package com.beautymirror.app.settings

import com.beautymirror.app.tracking.FaceTrackingResult
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Detects the start of a visitor session without treating brief landmark misses as a new face.
 *
 * A normal leave/re-enter sequence is detected through the absence timer. A direct hand-off where
 * MediaPipe switches from one visitor to another without reporting NO_FACE is also detected from a
 * sustained, implausibly large jump in normalized face position/scale. This avoids requiring face
 * recognition or storing biometric identity data.
 */
class VisitorSessionDetector(
    private val rearmAfterSeconds: Float = 0.65f,
) {
    private var absenceSeconds = rearmAfterSeconds
    private var armed = true
    private var visibleLastFrame = false
    private var previousCenterX = 0.5f
    private var previousCenterY = 0.5f
    private var previousArea = 0f
    private var discontinuityFrames = 0

    /** Returns true exactly once when a new sustained face appears. */
    fun update(tracking: FaceTrackingResult, deltaSeconds: Float): Boolean {
        val dt = deltaSeconds.coerceIn(0f, 0.10f)
        val visible = tracking.isValid &&
            tracking.effectOpacity >= 0.32f &&
            tracking.bounds.width >= 0.08f &&
            tracking.bounds.height >= 0.10f

        if (!visible) {
            absenceSeconds += dt
            if (absenceSeconds >= rearmAfterSeconds) armed = true
            visibleLastFrame = false
            discontinuityFrames = 0
            return false
        }

        val area = (tracking.bounds.width * tracking.bounds.height).coerceAtLeast(0.0001f)
        var directHandoff = false
        var discontinuity = false
        if (visibleLastFrame && previousArea > 0f) {
            val centerJump = hypot(
                (tracking.bounds.centerX - previousCenterX).toDouble(),
                (tracking.bounds.centerY - previousCenterY).toDouble(),
            ).toFloat()
            val scaleRatio = max(area / previousArea, previousArea / area)
            val previousLinearSize = sqrt(previousArea)
            discontinuity = centerJump > max(0.30f, previousLinearSize * 0.92f) ||
                (centerJump > 0.16f && scaleRatio > 2.20f)
            discontinuityFrames = if (discontinuity) discontinuityFrames + 1 else 0
            directHandoff = discontinuityFrames >= 2
        }

        val appeared = (armed && !visibleLastFrame) || directHandoff
        if (appeared) {
            armed = false
            discontinuityFrames = 0
        }
        absenceSeconds = 0f
        visibleLastFrame = true
        // During a possible direct hand-off, keep the old face as the reference for one extra
        // result. Updating immediately would make the second replacement frame look continuous.
        if (!discontinuity || directHandoff || previousArea <= 0f) {
            previousCenterX = tracking.bounds.centerX
            previousCenterY = tracking.bounds.centerY
            previousArea = area
        }
        return appeared
    }

    fun forceRearm() {
        armed = true
        visibleLastFrame = false
        absenceSeconds = rearmAfterSeconds
        previousArea = 0f
        discontinuityFrames = 0
    }
}
