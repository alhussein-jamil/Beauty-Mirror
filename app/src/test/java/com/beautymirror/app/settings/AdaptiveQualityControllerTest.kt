package com.beautymirror.app.settings

import com.beautymirror.app.rendering.FrameTimingCollector
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdaptiveQualityControllerTest {
    @Test
    fun neverPromotesAboveCeiling() {
        val c = AdaptiveQualityController(targetFrameMs = 33.3)
        c.setLevel(QualityLevel.MEDIUM)
        var now = 1_000L
        val snap = FrameTimingCollector.Snapshot(
            cameraFps = 30.0,
            analysisFps = 20.0,
            gpuFrameMs = 10.0,
            passMs = emptyMap(),
            droppedFrames = 0L,
        )
        var promoted: QualityLevel? = null
        repeat(20) {
            now += 500
            c.evaluate(now, snap)?.let { promoted = it }
        }
        assertThat(c.current()).isEqualTo(QualityLevel.MEDIUM)
        assertThat(promoted).isNull()
    }

    @Test
    fun dropsWhenOverBudget() {
        val c = AdaptiveQualityController(targetFrameMs = 33.3)
        c.setLevel(QualityLevel.HIGH)
        var now = 1_000L
        val snap = FrameTimingCollector.Snapshot(
            cameraFps = 18.0,
            analysisFps = 12.0,
            gpuFrameMs = 50.0,
            passMs = emptyMap(),
            droppedFrames = 0L,
        )
        now += 500
        assertThat(c.evaluate(now, snap)).isNull()
        now += 500
        val dropped = c.evaluate(now, snap)
        assertThat(dropped).isEqualTo(QualityLevel.MEDIUM)
        assertThat(c.current()).isEqualTo(QualityLevel.MEDIUM)
    }

    @Test
    fun lowDropsToPerformanceWhenOverBudget() {
        val c = AdaptiveQualityController(targetFrameMs = 33.3)
        c.setLevel(QualityLevel.LOW)
        var now = 1_000L
        val snap = FrameTimingCollector.Snapshot(
            cameraFps = 12.0,
            analysisFps = 8.0,
            gpuFrameMs = 60.0,
            passMs = emptyMap(),
            droppedFrames = 0L,
        )
        now += 500
        assertThat(c.evaluate(now, snap)).isNull()
        now += 500
        val dropped = c.evaluate(now, snap)
        assertThat(dropped).isEqualTo(QualityLevel.PERFORMANCE)
        assertThat(c.current()).isEqualTo(QualityLevel.PERFORMANCE)
    }

    @Test
    fun performanceIsTheFloor() {
        val c = AdaptiveQualityController(targetFrameMs = 33.3)
        c.setLevel(QualityLevel.LOW)
        c.applyAdaptive(QualityLevel.PERFORMANCE)
        var now = 1_000L
        val snap = FrameTimingCollector.Snapshot(
            cameraFps = 12.0,
            analysisFps = 4.0,
            gpuFrameMs = 70.0,
            passMs = emptyMap(),
            droppedFrames = 0L,
        )
        repeat(10) {
            now += 500
            assertThat(c.evaluate(now, snap)).isNull()
        }
        assertThat(c.current()).isEqualTo(QualityLevel.PERFORMANCE)
    }

    @Test
    fun ignoresZeroGpuSamples() {
        val c = AdaptiveQualityController(targetFrameMs = 33.3)
        c.setLevel(QualityLevel.LOW)
        var now = 1_000L
        val snap = FrameTimingCollector.Snapshot(30.0, 15.0, 0.0, emptyMap(), 0L)
        repeat(20) {
            now += 500
            assertThat(c.evaluate(now, snap)).isNull()
        }
        assertThat(c.current()).isEqualTo(QualityLevel.LOW)
    }

    @Test
    fun demotesWhenAnalysisStarved() {
        val c = AdaptiveQualityController(targetFrameMs = 33.3)
        c.setLevel(QualityLevel.HIGH) // analysisHz ~22
        var now = 1_000L
        val snap = FrameTimingCollector.Snapshot(
            cameraFps = 30.0,
            analysisFps = 5.0,
            gpuFrameMs = 20.0,
            passMs = emptyMap(),
            droppedFrames = 0L,
        )
        var dropped: QualityLevel? = null
        repeat(4) {
            now += 500
            c.evaluate(now, snap)?.let { if (dropped == null) dropped = it }
        }
        assertThat(dropped).isEqualTo(QualityLevel.MEDIUM)
    }

    @Test
    fun promotesUnderCeilingWhenComfortable() {
        val c = AdaptiveQualityController(targetFrameMs = 33.3)
        c.setLevel(QualityLevel.HIGH) // ceiling HIGH
        c.applyAdaptive(QualityLevel.LOW)
        var now = 1_000L
        val snap = FrameTimingCollector.Snapshot(
            cameraFps = 30.0,
            analysisFps = 20.0,
            gpuFrameMs = 12.0,
            passMs = emptyMap(),
            droppedFrames = 0L,
        )
        var firstPromote: QualityLevel? = null
        repeat(18) {
            now += 500
            c.evaluate(now, snap)?.let { if (firstPromote == null) firstPromote = it }
        }
        assertThat(firstPromote).isEqualTo(QualityLevel.MEDIUM)
        assertThat(c.current()).isEqualTo(QualityLevel.MEDIUM)
    }
}
