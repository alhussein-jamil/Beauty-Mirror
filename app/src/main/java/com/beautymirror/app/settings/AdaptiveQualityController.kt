package com.beautymirror.app.settings

import com.beautymirror.app.rendering.FrameTimingCollector

/**
 * Protects a 30 FPS presentation target with hysteresis.
 *
 * Demotion is deliberately fast: an exhibition should lose optional refinement before it becomes
 * visibly choppy. Promotion is slow to avoid oscillation. The user's selected quality remains a
 * ceiling; PERFORMANCE is an automatic emergency profile below LOW.
 */
class AdaptiveQualityController(
    private val targetFrameMs: Double = 33.3,
) {
    private var level: QualityLevel = QualityLevel.MEDIUM
    private var ceiling: QualityLevel = QualityLevel.HIGH
    private var overBudgetMs = 0L
    private var severeOverBudgetMs = 0L
    private var underBudgetMs = 0L
    private var lastEval = 0L

    fun current(): QualityLevel = level

    fun ceiling(): QualityLevel = ceiling

    fun isProtectingFrameRate(): Boolean = rank(level) < rank(ceiling)

    fun setLevel(level: QualityLevel) {
        val selectable = if (level.userSelectable) level else QualityLevel.LOW
        this.level = selectable
        this.ceiling = selectable
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

        val cameraBelowTarget = snapshot.cameraFps in 1.0..28.7
        val cameraSeverelyLow = snapshot.cameraFps in 1.0..23.5
        val renderOverBudget =
            snapshot.gpuFrameMs > targetFrameMs * 0.96 ||
                snapshot.p95FrameMs > targetFrameMs * 1.04 ||
                snapshot.slowFrameRatio > 0.12
        val renderSeverelyOver =
            snapshot.gpuFrameMs > targetFrameMs * 1.22 ||
                snapshot.p95FrameMs > targetFrameMs * 1.42 ||
                snapshot.slowFrameRatio > 0.32
        val analysisStarved = snapshot.analysisFps > 0.0 &&
            snapshot.analysisFps < level.analysisHz * 0.58

        val overloaded = cameraBelowTarget || renderOverBudget || analysisStarved
        val severe = cameraSeverelyLow || renderSeverelyOver
        val comfortable =
            snapshot.cameraFps >= 29.4 &&
                snapshot.gpuFrameMs in 0.1..(targetFrameMs * 0.62) &&
                snapshot.p95FrameMs in 0.1..(targetFrameMs * 0.76) &&
                snapshot.slowFrameRatio < 0.02 &&
                !analysisStarved

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

        // A severe stall gets one quality step within about half a second. A normal miss gets a
        // step after ~1.25 s, fast enough to recover before visitors perceive sustained judder.
        if (severeOverBudgetMs >= 500L || overBudgetMs >= 1_250L) {
            val next = lower(level)
            resetWindows()
            if (next != level) {
                level = next
                return next
            }
        }

        // Require a long comfortable window before restoring optional work.
        if (underBudgetMs >= 8_000L) {
            val next = minQuality(higher(level), ceiling)
            resetWindows()
            if (next != level) {
                level = next
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
}
