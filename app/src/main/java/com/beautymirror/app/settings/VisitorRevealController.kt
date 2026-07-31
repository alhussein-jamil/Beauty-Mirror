package com.beautymirror.app.settings

import kotlin.math.exp

/**
 * Stable, frame-rate-independent transition used when a visitor enters the tracked reflection.
 *
 * The reveal reaches roughly 99% within [attackSeconds], which keeps the exhibition interaction
 * under one second without a visible snap. Loss of tracking fades more slowly so a single missed
 * detector frame never makes the water pulse.
 */
class VisitorRevealController(
    private val attackSeconds: Float = 0.72f,
    private val releaseSeconds: Float = 1.10f,
) {
    var value: Float = 0f
        private set

    fun update(targetPresence: Float, deltaSeconds: Float): Float {
        val target = targetPresence.coerceIn(0f, 1f)
        val duration = if (target > value) attackSeconds else releaseSeconds
        val dt = deltaSeconds.coerceIn(0f, 0.10f)
        if (duration <= 0f || dt <= 0f) return value

        // e^-4.6 ~= 0.01, therefore 99% of the transition completes in one duration.
        val alpha = (1.0 - exp((-4.6f * dt / duration).toDouble())).toFloat()
        value += (target - value) * alpha.coerceIn(0f, 1f)
        if (kotlin.math.abs(target - value) < 0.001f) value = target
        return value
    }

    fun reset(value: Float = 0f) {
        this.value = value.coerceIn(0f, 1f)
    }
}
