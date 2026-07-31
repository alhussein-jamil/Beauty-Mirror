package com.beautymirror.app.settings

import com.beautymirror.app.rendering.FrameTimingCollector
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdaptiveQualityControllerTest {
    private fun snapshot(
        cameraFps: Double = 30.0,
        analysisFps: Double = 20.0,
        gpuFrameMs: Double = 12.0,
        p95FrameMs: Double = gpuFrameMs,
        slowRatio: Double = 0.0,
    ) = FrameTimingCollector.Snapshot(
        cameraFps = cameraFps,
        analysisFps = analysisFps,
        gpuFrameMs = gpuFrameMs,
        passMs = emptyMap(),
        droppedFrames = 0L,
        p95FrameMs = p95FrameMs,
        slowFrameRatio = slowRatio,
    )

    @Test
    fun pressureInterpolatesBeforeTierDrop() {
        val controller = AdaptiveQualityController()
        controller.setLevel(QualityLevel.HIGH)
        var now = 1_000L
        val overloaded = snapshot(cameraFps = 25.0, gpuFrameMs = 37.0, p95FrameMs = 42.0, slowRatio = 0.18)

        now += 500
        assertThat(controller.evaluate(now, overloaded)).isNull()
        now += 500
        assertThat(controller.evaluate(now, overloaded)).isNull()

        val state = controller.performanceState()
        assertThat(state.pressure).isGreaterThan(0f)
        assertThat(state.sampleScale).isLessThan(1f)
        assertThat(state.optionalScale).isLessThan(1f)
        assertThat(controller.current()).isEqualTo(QualityLevel.HIGH)
    }

    @Test
    fun sustainedSevereLoadEventuallyDropsTier() {
        val controller = AdaptiveQualityController()
        controller.setLevel(QualityLevel.HIGH)
        var now = 1_000L
        val overloaded = snapshot(cameraFps = 16.0, analysisFps = 5.0, gpuFrameMs = 58.0, p95FrameMs = 74.0, slowRatio = 0.48)
        var dropped: QualityLevel? = null
        for (i in 0 until 8) {
            now += 500
            val next = controller.evaluate(now, overloaded) ?: continue
            dropped = next
            break
        }
        assertThat(dropped).isEqualTo(QualityLevel.MEDIUM)
        assertThat(controller.current()).isEqualTo(QualityLevel.MEDIUM)
    }

    @Test
    fun recoveryIsSlowerThanDegradation() {
        val controller = AdaptiveQualityController()
        controller.setLevel(QualityLevel.HIGH)
        var now = 1_000L
        val overloaded = snapshot(cameraFps = 20.0, gpuFrameMs = 48.0, p95FrameMs = 61.0, slowRatio = 0.35)
        repeat(4) {
            now += 500
            controller.evaluate(now, overloaded)
        }
        val highPressure = controller.performanceState().pressure
        assertThat(highPressure).isGreaterThan(0.4f)

        val comfortable = snapshot()
        now += 500
        controller.evaluate(now, comfortable)
        val afterOneComfortableWindow = controller.performanceState().pressure
        assertThat(afterOneComfortableWindow).isGreaterThan(0.25f)
        assertThat(afterOneComfortableWindow).isLessThan(highPressure)
    }

    @Test
    fun neverPromotesAboveUserCeiling() {
        val controller = AdaptiveQualityController()
        controller.setLevel(QualityLevel.MEDIUM)
        var now = 1_000L
        repeat(32) {
            now += 500
            controller.evaluate(now, snapshot())
        }
        assertThat(controller.current()).isEqualTo(QualityLevel.MEDIUM)
    }

    @Test
    fun performanceIsFloor() {
        val controller = AdaptiveQualityController()
        controller.setLevel(QualityLevel.LOW)
        controller.applyAdaptive(QualityLevel.PERFORMANCE)
        var now = 1_000L
        val overloaded = snapshot(cameraFps = 10.0, analysisFps = 3.0, gpuFrameMs = 80.0, p95FrameMs = 95.0, slowRatio = 0.8)
        repeat(12) {
            now += 500
            assertThat(controller.evaluate(now, overloaded)).isNull()
        }
        assertThat(controller.current()).isEqualTo(QualityLevel.PERFORMANCE)
    }
    @Test
    fun lowCameraFpsWithCheapRendererIsRecognizedAsCameraLimited() {
        val controller = AdaptiveQualityController()
        controller.setLevel(QualityLevel.HIGH)
        var now = 1_000L
        val sensorLimited = snapshot(
            cameraFps = 24.0,
            analysisFps = 14.0,
            gpuFrameMs = 10.0,
            p95FrameMs = 14.0,
            slowRatio = 0.0,
        )
        repeat(11) {
            now += 500
            controller.evaluate(now, sensorLimited)
        }
        assertThat(controller.performanceState().cameraLimited).isTrue()
        assertThat(controller.current()).isEqualTo(QualityLevel.HIGH)
    }

}
