package com.beautymirror.app.util

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.android.asCoroutineDispatcher

class AndroidDispatchers {
    val main: CoroutineDispatcher =
        Handler(Looper.getMainLooper()).asCoroutineDispatcher("main")

    private val analysisThread = HandlerThread("bm-analysis").also { it.start() }
    val analysisHandler: Handler = Handler(analysisThread.looper)
    val analysisExecutor: Executor = Executor { command ->
        if (!analysisHandler.post(command)) {
            throw IllegalStateException("Analysis thread is shutting down")
        }
    }
    val analysis: CoroutineDispatcher = analysisHandler.asCoroutineDispatcher("analysis")

    private val cameraExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "bm-camera").also { it.isDaemon = true }
    }
    val camera: Executor = cameraExecutor

    private val glThread = HandlerThread("bm-gl").also { it.start() }
    val glHandler: Handler = Handler(glThread.looper)
    val gl: CoroutineDispatcher = glHandler.asCoroutineDispatcher("gl")

    fun runAnalysisBlocking(timeoutMs: Long = 2000L, block: () -> Unit): Boolean {
        if (analysisHandler.looper.isCurrentThread) {
            block()
            return true
        }
        val latch = CountDownLatch(1)
        var failure: Throwable? = null
        if (!analysisHandler.post {
                try {
                    block()
                } catch (t: Throwable) {
                    failure = t
                } finally {
                    latch.countDown()
                }
            }
        ) {
            return false
        }
        val completed = latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        failure?.let { throw it }
        return completed
    }

    fun release() {
        analysisThread.quitSafely()
        glThread.quitSafely()
        cameraExecutor.shutdown()
    }
}
