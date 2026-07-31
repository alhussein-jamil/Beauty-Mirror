package com.beautymirror.app.settings

import com.beautymirror.app.rendering.FrameTimingCollector
import kotlin.math.max

/**
 * Protects a 30 FPS presentation target with a continuous pressure signal plus rare tier changes.
 *
 * Pressure reacts quickly to overload and recovers slowly. The renderer uses it to interpolate
 * sample count, mask cadence and optional effects; discrete resolution tiers are changed only after
 * sustained pressure so visitors do not see constant camera reconfiguration.
 */
class AdaptiveQualityController(
    private val targetFrameMs: Double = 33.3,
) {
    private var level: QualityLevel = QualityLevel.MEDIUM
    private var ceiling: QualityLevel = QualityLevel.HIGH
    private var pressure = 0f
    private var overBudgetMs = 0L
    private var severeOverBudgetMs = 0L
    private var underBudgetMs = 0L
    private var cameraLimitEvidenceMs = 0L
    private var cameraLimited = false
    private var lastEval = 0L

    fun current(): QualityLevel = level

    fun ceiling(): QualityLevel = ceiling

    fun isProtectingFrameRate(): Boolean = pressure > 0.08f || rank(level) < rank(ceiling)

    fun performanceState(): AdaptivePerformanceState {
        val p = pressure.coerceIn(0f, 1f)
        val sampleScale = (1f - p * 0.62f).coerceIn(0.34f, 1f)
        val optionalScale = (1f - smoothStep(0.12f, 0.94f, p)).coerceIn(0.06f, 1f)
        val maskInterval = (p * p * 92f).toLong()
        return AdaptivePerformanceState(
            pressure = p,
            sampleScale = sampleScale,
            optionalScale = optionalScale,
            maskRefreshIntervalMs = maskInterval,
            protecting = isProtectingFrameRate(),
            cameraLimited = cameraLimited,
        )
    }

    fun setLevel(level: QualityLevel) {
        val selectable = if (level.userSelectable) level else QualityLevel.LOW
        this.level = selectable
        this.ceiling = selectable
        pressure = 0f
        cameraLimited = false
        cameraLimitEvidenceMs = 0L
        resetWindows()
    }

    /** Soft apply after an adaptive step without raising the user ceiling. */
    fun applyAdaptive(level: QualityLevel) {
        this.level = level
        resetWindows()
    }

    fun evaluate(nowMs: Long, snapshot: FrameTimingCollector.Snapshot): QualityLevel? {
        if (lastEval == 0L) {
            lastEval = nowMs
            return null
        }
        val dt = (nowMs - lastEval).coerceIn(0L, 2_000L)
        lastEval = nowMs
        if (snapshot.gpuFrameMs <= 0.0 && snapshot.cameraFps <= 0.0) return null

        val frameLoad = when {
            snapshot.gpuFrameMs > 0.0 -> snapshot.gpuFrameMs / targetFrameMs
            else -> 0.0
        }
        val p95Load = when {
            snapshot.p95FrameMs > 0.0 -> snapshot.p95FrameMs / targetFrameMs
            else -> 0.0
        }
        val rendererComfortable = snapshot.gpuFrameMs in 0.1..(targetFrameMs * 0.58) &&
            (snapshot.p95FrameMs <= 0.0 || snapshot.p95FrameMs < targetFrameMs * 0.72) &&
            snapshot.slowFrameRatio < 0.025
        val lowCameraRate = snapshot.cameraFps in 1.0..28.6
        if (lowCameraRate && rendererComfortable) {
            cameraLimitEvidenceMs += dt
            if (cameraLimitEvidenceMs >= 4_000L) cameraLimited = true
        } else {
            cameraLimitEvidenceMs = (cameraLimitEvidenceMs - dt * 2).coerceAtLeast(0L)
            if (!lowCameraRate || !rendererComfortable) cameraLimited = false
        }

        val fpsLoad = when {
            snapshot.cameraFps <= 0.0 || snapshot.cameraFps >= 30.0 -> 0.0
            cameraLimited || rendererComfortable -> 0.08 // Sensor cadence cannot be repaired by deleting GPU work.
            else -> ((30.0 - snapshot.cameraFps) / 11.0).coerceIn(0.0, 1.35)
        }
        val slowLoad = (snapshot.slowFrameRatio / 0.28).coerceIn(0.0, 1.35)
        val analysisLoad = if (
            snapshot.analysisFps > 0.0 && snapshot.analysisFps < level.analysisHz * 0.62
        ) {
            ((level.analysisHz * 0.62 - snapshot.analysisFps) / (level.analysisHz * 0.62))
                .coerceIn(0.0, 1.0)
        } else {
            0.0
        }

        val rawLoad = max(max(frameLoad, p95Load * 0.92), max(fpsLoad, max(slowLoad, analysisLoad)))
        val desiredPressure = smoothStep(0.72f, 1.34f, rawLoad.toFloat())
        val alpha = if (desiredPressure > pressure) {
            // Fast response: about one second from clear to strong protection at 500 ms sampling.
            0.42f
        } else {
            // Slow recovery prevents visible oscillation as visitors move in and out.
            0.075f
        }
        pressure += (desiredPressure - pressure) * alpha
        if (pressure < 0.015f) pressure = 0f

        val severe = rawLoad > 1.42 || snapshot.slowFrameRatio > 0.34
        val overloaded = pressure > 0.68f || rawLoad > 1.02
        val comfortable = pressure < 0.16f &&
            (snapshot.cameraFps >= 29.2 || cameraLimited) &&
            snapshot.gpuFrameMs in 0.1..(targetFrameMs * 0.68) &&
            snapshot.p95FrameMs in 0.1..(targetFrameMs * 0.82) &&
            snapshot.slowFrameRatio < 0.025

        if (overloaded) {
            overBudgetMs += dt
            severeOverBudgetMs = if (severe) severeOverBudgetMs + dt else 0L
            underBudgetMs = 0L
        } else if (comfortable) {
            underBudgetMs += dt
            overBudgetMs = (overBudgetMs - dt).coerceAtLeast(0L)
            severeOverBudgetMs = 0L
        } else {
            overBudgetMs = (overBudgetMs - dt / 2).coerceAtLeast(0L)
            severeOverBudgetMs = (severeOverBudgetMs - dt).coerceAtLeast(0L)
            underBudgetMs = (underBudgetMs - dt / 2).coerceAtLeast(0L)
        }

        // Continuous shedding is already active; only rebind resolution after sustained stress.
        if (severeOverBudgetMs >= 1_000L || overBudgetMs >= 3_000L) {
            val next = lower(level)
            resetWindows()
            if (next != level) {
                level = next
                pressure = (pressure * 0.72f).coerceAtLeast(0.30f)
                return next
            }
        }

        // Restore resolution only after a long stable window.
        if (underBudgetMs >= 12_000L) {
            val next = minQuality(higher(level), ceiling)
            resetWindows()
            if (next != level) {
                level = next
                pressure = 0.12f
                return next
            }
        }
        return null
    }

    private fun resetWindows() {
        overBudgetMs = 0L
        severeOverBudgetMs = 0L
        underBudgetMs = 0L
    }

    private fun lower(value: QualityLevel): QualityLevel = when (value) {
        QualityLevel.HIGH -> QualityLevel.MEDIUM
        QualityLevel.MEDIUM -> QualityLevel.LOW
        QualityLevel.LOW -> QualityLevel.PERFORMANCE
        QualityLevel.PERFORMANCE -> QualityLevel.PERFORMANCE
    }

    private fun higher(value: QualityLevel): QualityLevel = when (value) {
        QualityLevel.PERFORMANCE -> QualityLevel.LOW
        QualityLevel.LOW -> QualityLevel.MEDIUM
        QualityLevel.MEDIUM -> QualityLevel.HIGH
        QualityLevel.HIGH -> QualityLevel.HIGH
    }

    private fun minQuality(a: QualityLevel, b: QualityLevel): QualityLevel =
        if (rank(a) <= rank(b)) a else b

    private fun rank(value: QualityLevel): Int = when (value) {
        QualityLevel.PERFORMANCE -> 0
        QualityLevel.LOW -> 1
        QualityLevel.MEDIUM -> 2
        QualityLevel.HIGH -> 3
    }

    private fun smoothStep(edge0: Float, edge1: Float, value: Float): Float {
        if (edge0 == edge1) return if (value < edge0) 0f else 1f
        val x = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }
}
