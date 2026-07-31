package com.beautymirror.app.rendering

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FrameTimingCollectorTest {
    @Test
    fun snapshotReportsPassTimingsAfterWindow() {
        var now = 0L
        val c = FrameTimingCollector(windowMs = 100L, clock = { now })
        c.recordPass("skin", 2.0)
        c.recordPass("skin", 3.0)
        c.recordPass("lighting", 1.0)
        c.recordGpuFrame(8.0)
        c.recordGpuFrame(10.0)
        c.recordGpuFrame(40.0)
        c.markCameraFrame()
        c.markCameraFrame()
        c.markAnalysisFrame()
        now = 150L
        c.markCameraFrame() // triggers roll
        val snap = c.snapshot()
        assertThat(snap.gpuFrameMs).isWithin(0.01).of(58.0 / 3.0)
        assertThat(snap.p95FrameMs).isWithin(0.01).of(10.0)
        assertThat(snap.slowFrameRatio).isWithin(0.01).of(1.0 / 3.0)
        assertThat(snap.passMs).containsKey("skin")
        assertThat(snap.passMs["skin"]!!).isWithin(0.01).of(2.5)
        assertThat(snap.cameraFps).isGreaterThan(0.0)
    }
}
