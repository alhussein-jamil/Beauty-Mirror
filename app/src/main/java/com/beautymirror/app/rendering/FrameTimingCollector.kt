package com.beautymirror.app.rendering

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class FrameTimingCollector(
    private val windowMs: Long = 1000L,
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
) {
    data class Snapshot(
        val cameraFps: Double,
        val analysisFps: Double,
        val gpuFrameMs: Double,
        val passMs: Map<String, Double>,
        val droppedFrames: Long,
        val p95FrameMs: Double = gpuFrameMs,
        val slowFrameRatio: Double = 0.0,
    )

    private val passSums = LinkedHashMap<String, Double>()
    private val passSamples = LinkedHashMap<String, Int>()
    private val cameraFrames = AtomicInteger(0)
    private val analysisFrames = AtomicInteger(0)
    private var windowStart = clock()
    private var gpuSumMs = 0.0
    private var gpuSamples = 0
    private var slowFrames = 0
    private var frameSamples = DoubleArray(256)
    private var frameSampleCount = 0
    private val dropped = AtomicLong(0)

    fun markCameraFrame() {
        cameraFrames.incrementAndGet()
        rollIfNeeded()
    }

    fun markAnalysisFrame() {
        analysisFrames.incrementAndGet()
        rollIfNeeded()
    }

    fun markDropped() {
        dropped.incrementAndGet()
    }

    @Synchronized
    fun recordPass(name: String, ms: Double) {
        if (!ms.isFinite() || ms < 0.0) return
        passSums[name] = (passSums[name] ?: 0.0) + ms
        passSamples[name] = (passSamples[name] ?: 0) + 1
    }

    @Synchronized
    fun recordGpuFrame(ms: Double) {
        if (!ms.isFinite() || ms < 0.0) return
        gpuSumMs += ms
        gpuSamples++
        if (ms > 33.3) slowFrames++
        if (frameSampleCount >= frameSamples.size) {
            frameSamples = frameSamples.copyOf(frameSamples.size * 2)
        }
        frameSamples[frameSampleCount++] = ms
    }

    @Volatile
    private var snapshot = Snapshot(0.0, 0.0, 0.0, emptyMap(), 0)

    @Synchronized
    private fun rollIfNeeded() {
        val now = clock()
        val dt = now - windowStart
        if (dt < windowMs) return

        val sec = (dt / 1000.0).coerceAtLeast(1e-3)
        val avgGpu = if (gpuSamples > 0) gpuSumMs / gpuSamples else 0.0
        val p95 = if (frameSampleCount > 0) {
            val sorted = frameSamples.copyOf(frameSampleCount)
            sorted.sort()
            val index = ((sorted.size - 1) * 0.95).toInt().coerceIn(0, sorted.lastIndex)
            sorted[index]
        } else {
            0.0
        }
        val slowRatio = if (gpuSamples > 0) slowFrames.toDouble() / gpuSamples else 0.0
        val averagePasses = LinkedHashMap<String, Double>(passSums.size)
        for ((name, sum) in passSums) {
            val count = passSamples[name] ?: 0
            if (count > 0) averagePasses[name] = sum / count
        }
        snapshot = Snapshot(
            cameraFps = cameraFrames.getAndSet(0) / sec,
            analysisFps = analysisFrames.getAndSet(0) / sec,
            gpuFrameMs = avgGpu,
            passMs = averagePasses,
            droppedFrames = dropped.get(),
            p95FrameMs = p95,
            slowFrameRatio = slowRatio,
        )
        passSums.clear()
        passSamples.clear()
        gpuSumMs = 0.0
        gpuSamples = 0
        slowFrames = 0
        frameSampleCount = 0
        windowStart = now
    }

    fun snapshot(): Snapshot = snapshot
}
