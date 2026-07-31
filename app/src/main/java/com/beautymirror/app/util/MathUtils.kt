package com.beautymirror.app.util

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object MathUtils {
    fun clamp(value: Float, minV: Float, maxV: Float): Float = max(minV, min(maxV, value))

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = clamp((x - edge0) / (edge1 - edge0), 0f, 1f)
        return t * t * (3f - 2f * t)
    }

    fun distance(x0: Float, y0: Float, x1: Float, y1: Float): Float {
        val dx = x1 - x0
        val dy = y1 - y0
        return sqrt(dx * dx + dy * dy)
    }

    fun adaptiveAlpha(baseAlpha: Float, velocity: Float, slowThreshold: Float, fastThreshold: Float): Float {
        val t = clamp((velocity - slowThreshold) / max(1e-4f, fastThreshold - slowThreshold), 0f, 1f)
        return lerp(baseAlpha * 0.35f, min(1f, baseAlpha * 1.8f), t)
    }

    fun expSmooth(previous: Float, next: Float, alpha: Float): Float =
        alpha * next + (1f - alpha) * previous

    fun softKnee(value: Float, strength: Float): Float {
        // Bounded nonlinear response for exposure-like controls.
        return (exp(value * strength) - 1f) / (exp(strength) - 1f + 1e-4f)
    }

    /**
     * Soft activation for beauty strengths. Values inside [deadzone] become exact 0 so
     * the render graph can skip whole GPU passes; above that, response follows [softKnee].
     */
    fun effectAmount(value: Float, knee: Float = 1.6f, deadzone: Float = 0.012f): Float {
        val sign = if (value < 0f) -1f else 1f
        val abs = kotlin.math.abs(value)
        if (abs <= deadzone) return 0f
        val unit = abs.coerceIn(0f, 1f)
        val t = ((unit - deadzone) / (1f - deadzone)).coerceIn(0f, 1f)
        return sign * softKnee(t, knee)
    }
}
