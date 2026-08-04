package com.beautymirror.app.settings

import kotlin.math.exp

/**
 * Frame-rate-independent reveal used by the workshop installation.
 *
 * While a face remains present, [value] advances from zero to one over [durationSeconds]. The
 * curve is deliberately slow and readable: the visitor sees the reflection settle and the face
 * corrections accumulate instead of receiving an immediate filter. Brief detector misses hold the
 * progress; a real departure fades the reveal and arms the next visitor session.
 */
class VisitorRevealController(
    durationSeconds: Float = 10f,
    private val releaseSeconds: Float = 0.85f,
) {
    var durationSeconds: Float = durationSeconds.coerceIn(MIN_DURATION_SECONDS, MAX_DURATION_SECONDS)
        private set

    var value: Float = 0f
        private set

    private var elapsedPresentSeconds: Float = 0f

    fun setDuration(seconds: Float) {
        durationSeconds = seconds.coerceIn(MIN_DURATION_SECONDS, MAX_DURATION_SECONDS)
    }

    fun update(targetPresence: Float, deltaSeconds: Float): Float {
        val target = targetPresence.coerceIn(0f, 1f)
        val dt = deltaSeconds.coerceIn(0f, 0.10f)
        if (dt <= 0f) return value

        if (target > 0.12f) {
            // Presence confidence scales time only slightly. A partially occluded but tracked face
            // should continue progressing rather than visibly pausing the artwork.
            elapsedPresentSeconds += dt * (0.78f + target * 0.22f)
            val linear = (elapsedPresentSeconds / durationSeconds).coerceIn(0f, 1f)
            value = revealCurve(linear)
        } else {
            // Preserve progress across one or two missed detector frames, then fade naturally.
            val alpha = (1.0 - exp((-4.6f * dt / releaseSeconds).toDouble())).toFloat()
            value += (0f - value) * alpha.coerceIn(0f, 1f)
            if (value < 0.001f) {
                value = 0f
                elapsedPresentSeconds = 0f
            }
        }
        return value
    }

    fun restart() {
        elapsedPresentSeconds = 0f
        value = 0f
    }

    fun reset(value: Float = 0f) {
        this.value = value.coerceIn(0f, 1f)
        elapsedPresentSeconds = inverseSmootherStep(this.value) * durationSeconds
    }

    private fun smootherStep(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return x * x * x * (x * (x * 6f - 15f) + 10f)
    }

    // Keep a linear component so the first seconds are perceptible to workshop visitors, while the
    // smootherstep component preserves a calm beginning and landing instead of a mechanical fade.
    private fun revealCurve(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return x * 0.28f + smootherStep(x) * 0.72f
    }

    /** Small fixed-iteration inverse used only by tests/debug reset, never in the frame loop. */
    private fun inverseSmootherStep(value: Float): Float {
        var low = 0f
        var high = 1f
        repeat(10) {
            val mid = (low + high) * 0.5f
            if (revealCurve(mid) < value) low = mid else high = mid
        }
        return (low + high) * 0.5f
    }

    companion object {
        const val MIN_DURATION_SECONDS = 3f
        const val MAX_DURATION_SECONDS = 30f
    }
}
