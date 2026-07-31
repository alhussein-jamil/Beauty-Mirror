package com.beautymirror.app.rendering

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES30
import android.os.Handler
import android.util.Log
import android.view.Surface
import com.beautymirror.app.BuildConfig
import com.beautymirror.app.settings.AdaptivePerformanceState
import com.beautymirror.app.settings.BeautySettings
import com.beautymirror.app.settings.QualityLevel
import com.beautymirror.app.tracking.FaceTrackingResult
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns GL context + SurfaceTexture for camera OES frames and drives [RenderGraph].
 */
class BeautyRenderer(
    private val context: Context,
    private val glHandler: Handler,
) {
    interface FrameListener {
        fun onFramePresented()
    }

    private val alive = AtomicBoolean(false)
    private var egl: GlContextManager? = null
    private var graph: RenderGraph? = null
    private var oesTexture: GlTexture? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var cameraInputSurface: Surface? = null
    private var outputSurface: Surface? = null
    private var outputWidth = 1
    private var outputHeight = 1
    private var cameraInputWidth = 1
    private var cameraInputHeight = 1
    private var cameraInputRotationDegrees = 0
    private var surfaceContainsCameraTransform = false
    private var cameraTransformRequestsMirror = false
    private val texMatrix = FloatArray(16)
    private var hasTexMatrix = false
    private var rgbaScratch: java.nio.ByteBuffer? = null
    private var frameListener: FrameListener? = null
    // If setOutputSurface arrives before GL init, replay after initialize.
    private var pendingOutput: Triple<Surface, Int, Int>? = null

    @Volatile
    var settings: BeautySettings = BeautySettings.off()
        set(value) {
            field = value
            glHandler.post { graph?.settings = value }
        }

    /** Hold-to-compare from the dock — kept separate so settings writers cannot clear it. */
    @Volatile
    var compareHold: Boolean = false
        set(value) {
            field = value
            glHandler.post { graph?.compareHold = value }
        }

    @Volatile
    var performanceState: AdaptivePerformanceState = AdaptivePerformanceState.FULL
        set(value) {
            field = value
            glHandler.post { graph?.performanceState = value }
        }

    @Volatile
    var tracking: FaceTrackingResult = FaceTrackingResult.empty()
        set(value) {
            field = value
            glHandler.post {
                graph?.tracking = value
            }
        }

    @Volatile
    var isFrontCamera: Boolean = true
        set(value) {
            field = value
            updateMirrorState()
        }

    @Volatile
    var mirrorPreviewEnabled: Boolean = true
        set(value) {
            field = value
            updateMirrorState()
        }

    private fun updateMirrorState() {
        val desired = isFrontCamera && mirrorPreviewEnabled
        glHandler.post {
            graph?.setMirrorTransform(
                desiredMirror = desired,
                surfaceContainsCameraTransform = surfaceContainsCameraTransform,
                cameraTransformRequestsMirror = cameraTransformRequestsMirror,
            )
        }
    }

    fun setFrameListener(listener: FrameListener?) {
        frameListener = listener
    }

    fun initializeOnGlThread() {
        check(glHandler.looper.isCurrentThread)
        if (alive.get()) return
        val mgr = GlContextManager()
        mgr.initialize()
        mgr.makeCurrentPbuffer()
        egl = mgr
        graph = RenderGraph(context).also {
            it.settings = settings
            it.performanceState = performanceState
            it.setMirrorTransform(
                desiredMirror = isFrontCamera && mirrorPreviewEnabled,
                surfaceContainsCameraTransform = surfaceContainsCameraTransform,
                cameraTransformRequestsMirror = cameraTransformRequestsMirror,
            )
            it.setCameraInputTransform(
                cameraInputWidth,
                cameraInputHeight,
                cameraInputRotationDegrees,
            )
        }
        oesTexture = GlTexture.createOes()
        surfaceTexture = SurfaceTexture(oesTexture!!.id).also { st ->
            // Callback already runs on the GL handler; posting again would allow a backlog of
            // obsolete draw requests under load.
            st.setOnFrameAvailableListener({ drawFrame() }, glHandler)
        }
        cameraInputSurface = Surface(surfaceTexture)
        alive.set(true)
        pendingOutput?.let { (surface, w, h) ->
            applyOutputSurface(surface, w, h)
            pendingOutput = null
        }
        if (BuildConfig.DEBUG) Log.i(TAG, "BeautyRenderer initialized")
    }

    /**
     * Returns the persistent camera input surface after synchronously applying CameraX's
     * requested buffer size on the GL thread. This prevents device-dependent default-size
     * buffers and keeps the preview crop aligned with analysis landmarks.
     */
    fun getCameraInputSurface(width: Int, height: Int, timeoutMs: Long = 1500L): Surface {
        val safeW = width.coerceAtLeast(1)
        val safeH = height.coerceAtLeast(1)
        if (glHandler.looper.isCurrentThread) {
            return configureCameraInputOnGlThread(safeW, safeH)
        }
        val latch = CountDownLatch(1)
        var result: Surface? = null
        var failure: Throwable? = null
        val posted = glHandler.post {
            try {
                result = configureCameraInputOnGlThread(safeW, safeH)
            } catch (t: Throwable) {
                failure = t
            } finally {
                latch.countDown()
            }
        }
        check(posted) { "GL thread is not accepting work" }
        check(latch.await(timeoutMs, TimeUnit.MILLISECONDS)) { "Timed out preparing camera surface" }
        failure?.let { throw IllegalStateException("Failed to prepare camera surface", it) }
        return result ?: error("Renderer not initialized")
    }

    private fun configureCameraInputOnGlThread(width: Int, height: Int): Surface {
        check(glHandler.looper.isCurrentThread)
        val st = surfaceTexture ?: error("Renderer not initialized")
        cameraInputWidth = width
        cameraInputHeight = height
        st.setDefaultBufferSize(width, height)
        graph?.setCameraInputTransform(width, height, cameraInputRotationDegrees)
        return cameraInputSurface ?: error("Renderer not initialized")
    }

    /**
     * Updates the orientation metadata supplied by CameraX. SurfaceTexture applies the actual
     * rotation matrix; the render graph needs the angle to calculate the correct fill crop.
     */
    fun setCameraInputTransform(
        width: Int,
        height: Int,
        rotationDegrees: Int,
        surfaceContainsCameraTransform: Boolean = this.surfaceContainsCameraTransform,
        cameraTransformRequestsMirror: Boolean = this.cameraTransformRequestsMirror,
    ) {
        val safeW = width.coerceAtLeast(1)
        val safeH = height.coerceAtLeast(1)
        val safeRotation = ((rotationDegrees % 360) + 360) % 360
        glHandler.post {
            cameraInputWidth = safeW
            cameraInputHeight = safeH
            cameraInputRotationDegrees = safeRotation
            this.surfaceContainsCameraTransform = surfaceContainsCameraTransform
            this.cameraTransformRequestsMirror = cameraTransformRequestsMirror
            graph?.setCameraInputTransform(safeW, safeH, safeRotation)
            graph?.setMirrorTransform(
                desiredMirror = isFrontCamera && mirrorPreviewEnabled,
                surfaceContainsCameraTransform = surfaceContainsCameraTransform,
                cameraTransformRequestsMirror = cameraTransformRequestsMirror,
            )
        }
    }

    fun setOutputSurface(surface: Surface?, width: Int, height: Int) {
        glHandler.post {
            if (surface != null && egl == null) {
                pendingOutput = Triple(surface, width.coerceAtLeast(1), height.coerceAtLeast(1))
                return@post
            }
            if (surface == null) {
                pendingOutput = null
            }
            applyOutputSurface(surface, width.coerceAtLeast(1), height.coerceAtLeast(1))
        }
    }

    private fun applyOutputSurface(surface: Surface?, width: Int, height: Int) {
        outputSurface = surface
        if (surface != null) {
            outputWidth = width
            outputHeight = height
        }
        // When surface is null keep last outputWidth/Height so quality resizes stay sane.
        val mgr = egl ?: return
        if (surface != null) {
            mgr.makeCurrent(surface)
            val q = settings.qualityLevel
            val mask = q.maskResolution
            val targetH = q.previewTargetHeight
            val aspect = outputWidth.toFloat() / outputHeight.coerceAtLeast(1)
            val h = targetH.coerceAtMost(outputHeight)
            val w = (h * aspect).toInt().coerceAtLeast(1)
            graph?.resize(w, h, mask)
        } else {
            mgr.makeCurrentPbuffer()
        }
    }

    fun updateQuality(level: QualityLevel) {
        glHandler.post {
            settings = settings.copy(qualityLevel = level)
            graph?.settings = settings
            val aspect = outputWidth.toFloat() / outputHeight.coerceAtLeast(1)
            val h = level.previewTargetHeight.coerceAtMost(outputHeight.coerceAtLeast(level.previewTargetHeight))
            val w = (h * aspect).toInt().coerceAtLeast(1)
            graph?.resize(w, h, level.maskResolution)
        }
    }

    private fun drawFrame() {
        if (!alive.get()) return
        val st = surfaceTexture ?: return
        val g = graph ?: return
        val oes = oesTexture ?: return
        val mgr = egl ?: return
        try {
            // Always drain OES buffers even when display surface is gone.
            st.updateTexImage()
            st.getTransformMatrix(texMatrix)
            hasTexMatrix = true
            val out = outputSurface ?: return
            mgr.makeCurrent(out)
            g.settings = settings
            g.performanceState = performanceState
            g.tracking = tracking
            g.setMirrorTransform(
                desiredMirror = isFrontCamera && mirrorPreviewEnabled,
                surfaceContainsCameraTransform = surfaceContainsCameraTransform,
                cameraTransformRequestsMirror = cameraTransformRequestsMirror,
            )
            g.renderFrame(oes, texMatrix, outputWidth, outputHeight, toScreen = true)
            mgr.setPresentationTime(st.timestamp)
            mgr.swapBuffers()
            frameListener?.onFramePresented()
            GlShader.checkError("drawFrame")
        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) Log.e(TAG, "drawFrame failed", t)
        }
    }

    /**
     * Capture processed frame into RGBA [ByteBuffer]. Must run on GL thread.
     * Allowed for photo capture only — not used in preview loop.
     */
    fun captureRgba(width: Int, height: Int): ByteBuffer? {
        check(glHandler.looper.isCurrentThread)
        val g = graph ?: return null
        val oes = oesTexture ?: return null
        if (!hasTexMatrix) return null
        val prevW = outputWidth
        val prevH = outputHeight
        val mask = settings.qualityLevel.maskResolution
        val out = outputSurface
        fun restorePreview() {
            if (out == null) return
            val aspect = prevW.toFloat() / prevH.coerceAtLeast(1)
            val h = settings.qualityLevel.previewTargetHeight.coerceAtMost(prevH)
            val w = (h * aspect).toInt().coerceAtLeast(1)
            g.resize(w, h, mask)
            try {
                egl?.makeCurrent(out)
            } catch (_: Throwable) {
            }
        }
        return try {
            g.settings = settings
            g.performanceState = performanceState
            g.tracking = tracking
            g.setMirrorTransform(
                desiredMirror = isFrontCamera && mirrorPreviewEnabled,
                surfaceContainsCameraTransform = surfaceContainsCameraTransform,
                cameraTransformRequestsMirror = cameraTransformRequestsMirror,
            )
            // Reuse the last frame already consumed by the preview loop. Calling updateTexImage()
            // here can block waiting for a producer buffer and races the normal frame callback.
            g.resize(width, height, mask)
            g.renderFrame(oes, texMatrix, width, height, toScreen = false)
            if (!g.bindLastOutputForRead()) {
                g.unbindLastOutput()
                return null
            }
            val need = width * height * 4
            val buf = rgbaScratch?.takeIf { it.capacity() >= need }
                ?: ByteBuffer.allocateDirect(need).order(ByteOrder.nativeOrder()).also { rgbaScratch = it }
            buf.clear()
            buf.limit(need)
            GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buf)
            g.unbindLastOutput()
            buf.rewind()
            buf
        } finally {
            restorePreview()
        }
    }

    fun timingSnapshot(): FrameTimingCollector.Snapshot? = graph?.timing?.snapshot()

    fun markAnalysisFrame() {
        glHandler.post { graph?.timing?.markAnalysisFrame() }
    }

    fun markDropped() {
        glHandler.post { graph?.timing?.markDropped() }
    }

    fun release() {
        glHandler.post { releaseGlResources() }
    }

    /** Blocks until GL resources are freed, then safe to quit the GL thread. */
    fun releaseSync(timeoutMs: Long = 2000L) {
        if (glHandler.looper.isCurrentThread) {
            releaseGlResources()
            return
        }
        val latch = CountDownLatch(1)
        glHandler.post {
            try {
                releaseGlResources()
            } finally {
                latch.countDown()
            }
        }
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS) && BuildConfig.DEBUG) {
            Log.w(TAG, "releaseSync timed out after ${timeoutMs}ms")
        }
    }

    private fun releaseGlResources() {
        alive.set(false)
        pendingOutput = null
        surfaceTexture?.setOnFrameAvailableListener(null)
        surfaceTexture?.release()
        surfaceTexture = null
        cameraInputSurface?.release()
        cameraInputSurface = null
        oesTexture?.delete()
        oesTexture = null
        graph?.release()
        graph = null
        egl?.release()
        egl = null
    }

    companion object {
        private const val TAG = "BeautyRenderer"
    }
}
