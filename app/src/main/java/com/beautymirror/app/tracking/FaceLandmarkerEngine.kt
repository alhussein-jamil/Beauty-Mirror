package com.beautymirror.app.tracking

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.beautymirror.app.BuildConfig
import com.beautymirror.app.settings.QualityLevel
import com.beautymirror.app.util.MatrixUtils
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.framework.image.MediaImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.min

/**
 * Persistent MediaPipe Face Landmarker in LIVE_STREAM mode.
 * Never recreate per frame.
 *
 * Ownership: when [detectAsync] returns true, this engine owns [ImageProxy] until the
 * MediaPipe result/error callback closes it. Caller must not close an accepted frame.
 *
 * Coordinate contract:
 * - MediaImage path: MediaPipe rotates via ImageProcessingOptions; mapper uses rotation=0
 *   and upright analysis size (no second rotation).
 * - Bitmap fallback: bitmap is sensor-oriented; mapper applies [rotationDegrees].
 */
class FaceLandmarkerEngine(
    context: Context,
    private val mapper: FaceCoordinateMapper,
    private val smoother: LandmarkSmoother = LandmarkSmoother(),
) {
    interface Listener {
        fun onTracking(result: FaceTrackingResult)
        fun onError(message: String, error: Throwable?)
    }

    private val appContext = context.applicationContext
    private val ready = AtomicBoolean(false)
    private val landmarker = AtomicReference<FaceLandmarker?>(null)
    private var listener: Listener? = null
    private var lastAcceptMs: Long = 0L
    private var minIntervalMs: Long = 66L
    private var analysisDurationMs: Double = 0.0
    private var mediapipeDurationMs: Double = 0.0
    private var lastTimestampMs: Long = -1L
    private val inFlightConfig = AtomicReference<FaceCoordinateMapper.Config?>(null)
    @Volatile private var lastGoodConfig: FaceCoordinateMapper.Config? = null
    private val inFlightImage = AtomicReference<ImageProxy?>(null)
    private val busy = AtomicBoolean(false)
    private val droppedStaleResults = java.util.concurrent.atomic.AtomicLong(0)
    private val generation = java.util.concurrent.atomic.AtomicInteger(0)
    private val inFlightGeneration = java.util.concurrent.atomic.AtomicInteger(-1)
    private val inFlightTimestampMs = java.util.concurrent.atomic.AtomicLong(-1)

    private var nv21Scratch: ByteArray? = null
    private var scaledScratch: Bitmap? = null
    private val jpegScratch = java.io.ByteArrayOutputStream(64 * 1024)

    // Reusable cheek sample scratch (avoid per-frame arrays escaping).
    private val cheekLock = Any()
    private val leftCheekScratch = floatArrayOf(0.78f, 0.64f, 0.58f)
    private val rightCheekScratch = floatArrayOf(0.78f, 0.64f, 0.58f)
    private val normalizedScratch = ArrayList<LandmarkPoint>(478)

    val lastAnalysisDurationMs: Double get() = analysisDurationMs
    val lastMediapipeDurationMs: Double get() = mediapipeDurationMs
    val staleResultDrops: Long get() = droppedStaleResults.get()

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun setQuality(level: QualityLevel) {
        minIntervalMs = (1000f / level.analysisHz).toLong().coerceAtLeast(33L)
    }

    fun initialize(): Boolean {
        if (ready.get()) return true
        return try {
            val assetExists = runCatching { appContext.assets.open(MODEL_ASSET).close(); true }.getOrDefault(false)
            if (!assetExists) {
                listener?.onError("Missing asset $MODEL_ASSET", null)
                return false
            }
            createLandmarker(Delegate.GPU)
            ready.set(true)
            true
        } catch (gpuFail: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "GPU landmarker failed, trying CPU", gpuFail)
            try {
                createLandmarker(Delegate.CPU)
                ready.set(true)
                true
            } catch (t: Exception) {
                listener?.onError("Face landmarker init failed", t)
                false
            }
        }
    }

    private fun createLandmarker(delegate: Delegate) {
        val base = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET)
            .setDelegate(delegate)
            .build()
        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(base)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumFaces(1)
            .setOutputFaceBlendshapes(false)
            .setOutputFacialTransformationMatrixes(true)
            .setResultListener { result, _ -> onResult(result) }
            .setErrorListener { e ->
                val flightGeneration = inFlightGeneration.get()
                releaseInFlightImage()
                busy.set(false)
                if (flightGeneration == generation.get() && ready.get()) {
                    listener?.onError(e.message ?: "landmarker error", e)
                }
            }
            .build()
        landmarker.set(FaceLandmarker.createFromOptions(appContext, options))
    }

    /**
     * @return true if this engine took ownership of [image] (caller must not close it).
     */
    fun detectAsync(image: ImageProxy, rotationDegrees: Int): Boolean {
        if (!ready.get()) return false
        val detector = landmarker.get() ?: return false
        val now = SystemClock.elapsedRealtime()
        if (now - lastAcceptMs < minIntervalMs) return false
        if (!busy.compareAndSet(false, true)) return false
        lastAcceptMs = now
        val t0 = SystemClock.elapsedRealtimeNanos()

        var ts = image.imageInfo.timestamp / 1_000_000L
        if (ts <= lastTimestampMs) ts = lastTimestampMs + 1
        lastTimestampMs = ts

        val previewCfg = mapper.config
        sampleFaceMetrics(image, rotationDegrees)

        // Prefer zero-copy MediaImage. Engine owns ImageProxy until MediaPipe callback.
        val mediaImage = image.image
        if (mediaImage != null) {
            try {
                val uprightW = if (rotationDegrees % 180 == 0) image.width else image.height
                val uprightH = if (rotationDegrees % 180 == 0) image.height else image.width
                val cfg = previewCfg.copy(
                    rotationDegrees = 0,
                    analysisWidth = uprightW,
                    analysisHeight = uprightH,
                )
                inFlightConfig.set(cfg)
                inFlightGeneration.set(generation.get())
                inFlightTimestampMs.set(ts)
                takeOwnership(image)
                val mpImage: MPImage = MediaImageBuilder(mediaImage).build()
                val opts = ImageProcessingOptions.builder()
                    .setRotationDegrees(rotationDegrees)
                    .build()
                detector.detectAsync(mpImage, opts, ts)
                analysisDurationMs = (SystemClock.elapsedRealtimeNanos() - t0) / 1e6
                return true
            } catch (t: Exception) {
                if (BuildConfig.DEBUG) Log.w(TAG, "MediaImage detect failed, bitmap fallback", t)
                // Drop ownership so caller/fallback can close once.
                inFlightImage.compareAndSet(image, null)
            }
        }

        // Bitmap fallback: copy pixels, caller closes ImageProxy (return false).
        val bitmap = imageProxyToBitmap(image, maxSide = 640)
        if (bitmap == null) {
            busy.set(false)
            return false
        }
        val cfg = previewCfg.copy(
            rotationDegrees = rotationDegrees,
            analysisWidth = bitmap.width,
            analysisHeight = bitmap.height,
        )
        inFlightConfig.set(cfg)
        inFlightGeneration.set(generation.get())
        inFlightTimestampMs.set(ts)
        try {
            val mpImage: MPImage = BitmapImageBuilder(bitmap).build()
            val opts = ImageProcessingOptions.builder()
                .setRotationDegrees(0)
                .build()
            detector.detectAsync(mpImage, opts, ts)
            analysisDurationMs = (SystemClock.elapsedRealtimeNanos() - t0) / 1e6
            return false
        } catch (t: Exception) {
            busy.set(false)
            listener?.onError("detectAsync failed", t)
            return false
        }
    }

    fun onCameraChanged() {
        // Invalidate the result but do not close an ImageProxy that MediaPipe may still be reading.
        // The stale callback/error path releases it and clears busy before the new camera is analyzed.
        generation.incrementAndGet()
        smoother.reset()
        lastTimestampMs = -1L
        lastNormalized = emptyList()
        inFlightConfig.set(null)
        lastGoodConfig = null
        lastLeftCheekUv = LandmarkPoint(0.35f, 0.45f)
        lastRightCheekUv = LandmarkPoint(0.65f, 0.45f)
    }

    fun release() {
        ready.set(false)
        generation.incrementAndGet()
        // FaceLandmarker.close() drains native work. Release the ImageProxy only afterwards so
        // native code never observes an already-closed android.media.Image.
        runCatching { landmarker.getAndSet(null)?.close() }
        releaseInFlightImage()
        busy.set(false)
        smoother.reset()
        scaledScratch?.recycle()
        scaledScratch = null
        nv21Scratch = null
        jpegScratch.reset()
    }

    private fun takeOwnership(image: ImageProxy) {
        inFlightImage.getAndSet(image)?.close()
    }

    private fun releaseInFlightImage() {
        inFlightImage.getAndSet(null)?.close()
    }

    private fun onResult(result: FaceLandmarkerResult) {
        val t0 = SystemClock.elapsedRealtimeNanos()
        val resultTs = result.timestampMs()
        val resultGen = inFlightGeneration.get()
        try {
            // Timestamp mismatch = older flight after a newer detect started (camera switch race).
            if (resultTs != inFlightTimestampMs.get() || resultGen != generation.get()) {
                droppedStaleResults.incrementAndGet()
                return
            }
            val nowMs = SystemClock.elapsedRealtime()
            val faces = result.faceLandmarks()
            if (faces.isNullOrEmpty()) {
                val held = smoother.push(nowMs, null)
                listener?.onTracking(remapHeld(held))
                return
            }
            val landmarks = faces[0]
            normalizedScratch.clear()
            for (lm in landmarks) {
                normalizedScratch.add(LandmarkPoint(lm.x(), lm.y(), lm.z()))
            }
            val normalized = normalizedScratch
            var minX = 1f; var minY = 1f; var maxX = 0f; var maxY = 0f
            for (p in normalized) {
                minX = min(minX, p.x); minY = min(minY, p.y)
                maxX = max(maxX, p.x); maxY = max(maxY, p.y)
            }
            val cfg = cfgWithLivePreview(inFlightConfig.get() ?: lastGoodConfig ?: mapper.config)
            lastGoodConfig = cfg
            // Skip pre-smooth display/texture mapping — smoother only needs norms + bounds;
            // remap once after smooth (below).
            val displayBounds = mapper.mapBounds(minX, minY, maxX, maxY, cfg)
            val poseMatrix = result.facialTransformationMatrixes()
                .orElse(emptyList())
                .firstOrNull()
            val pose = FacePoseEstimator.estimate(normalized, poseMatrix, mirrorFront = cfg.mirrorFront)
            val raw = FaceTrackingResult(
                timestampNs = resultTs * 1_000_000L,
                bounds = displayBounds,
                normalizedLandmarks = normalized,
                displayLandmarks = emptyList(),
                textureLandmarks = emptyList(),
                trackingConfidence = 1f,
                presenceConfidence = 1f,
                headYaw = pose.yawDegrees,
                headPitch = pose.pitchDegrees,
                headRoll = pose.rollDegrees,
                isValid = true,
                timeSinceValidMs = 0L,
                state = TrackingState.TRACKING,
                effectOpacity = 1f,
                faceLuminance = lastFaceLuma,
                leftCheekColor = synchronized(cheekLock) { leftCheekScratch.copyOf() },
                rightCheekColor = synchronized(cheekLock) { rightCheekScratch.copyOf() },
                leftCheekUv = lastLeftCheekUv,
                rightCheekUv = lastRightCheekUv,
            )
            val smoothed = smoother.push(nowMs, raw)
            // Always remap from smoothed normalized landmarks — never keep raw display/texture.
            val norms = smoothed.normalizedLandmarks
            val (d, tex) = mapper.mapLandmarks(norms, cfg)
            var sMinX = 1f; var sMinY = 1f; var sMaxX = 0f; var sMaxY = 0f
            for (p in norms) {
                if (p.x < sMinX) sMinX = p.x
                if (p.y < sMinY) sMinY = p.y
                if (p.x > sMaxX) sMaxX = p.x
                if (p.y > sMaxY) sMaxY = p.y
            }
            val nudgedL = nudgeCheekUv(tex, CHEEK_LEFT, left = true)
            val nudgedR = nudgeCheekUv(tex, CHEEK_RIGHT, left = false)
            // Blend nudged UV with smoother output for temporal stability.
            val out = smoothed.copy(
                displayLandmarks = d,
                textureLandmarks = tex,
                bounds = if (norms.isNotEmpty()) mapper.mapBounds(sMinX, sMinY, sMaxX, sMaxY, cfg) else smoothed.bounds,
                leftCheekUv = LandmarkPoint(
                    smoothed.leftCheekUv.x * 0.35f + nudgedL.x * 0.65f,
                    smoothed.leftCheekUv.y * 0.35f + nudgedL.y * 0.65f,
                ),
                rightCheekUv = LandmarkPoint(
                    smoothed.rightCheekUv.x * 0.35f + nudgedR.x * 0.65f,
                    smoothed.rightCheekUv.y * 0.35f + nudgedR.y * 0.65f,
                ),
            )
            lastLeftCheekUv = out.leftCheekUv
            lastRightCheekUv = out.rightCheekUv
            lastNormalized = norms
            listener?.onTracking(out)
        } finally {
            // The matching callback owns the one allowed in-flight request, even if a camera
            // switch invalidated its generation while it was running.
            if (resultTs == inFlightTimestampMs.get()) {
                releaseInFlightImage()
                busy.set(false)
            }
            mediapipeDurationMs = (SystemClock.elapsedRealtimeNanos() - t0) / 1e6
        }
    }

    private fun remapHeld(held: FaceTrackingResult): FaceTrackingResult {
        if (held.normalizedLandmarks.isEmpty()) return held
        val cfg = cfgWithLivePreview(inFlightConfig.get() ?: lastGoodConfig ?: mapper.config)
        val (d, tex) = mapper.mapLandmarks(held.normalizedLandmarks, cfg)
        val nudgedL = nudgeCheekUv(tex, CHEEK_LEFT, left = true)
        val nudgedR = nudgeCheekUv(tex, CHEEK_RIGHT, left = false)
        return held.copy(
            displayLandmarks = d,
            textureLandmarks = tex,
            leftCheekUv = LandmarkPoint(
                held.leftCheekUv.x * 0.35f + nudgedL.x * 0.65f,
                held.leftCheekUv.y * 0.35f + nudgedL.y * 0.65f,
            ),
            rightCheekUv = LandmarkPoint(
                held.rightCheekUv.x * 0.35f + nudgedR.x * 0.65f,
                held.rightCheekUv.y * 0.35f + nudgedR.y * 0.65f,
            ),
        )
    }

    @Volatile
    private var lastFaceLuma: Float = 0.5f
    @Volatile
    private var lastNormalized: List<LandmarkPoint> = emptyList()
    @Volatile private var lastLeftCheekUv = LandmarkPoint(0.35f, 0.45f)
    @Volatile private var lastRightCheekUv = LandmarkPoint(0.65f, 0.45f)

    /**
     * Sample Y-plane luminance at cheek / forehead landmarks in sensor buffer space.
     * Cheap and allocation-light; drives face lighting + under-eye references.
     */
    private fun sampleFaceMetrics(
        image: ImageProxy,
        rotationDegrees: Int,
    ) {
        try {
            val yPlane = image.planes[0]
            val yBuf = yPlane.buffer.duplicate()
            val rowStride = yPlane.rowStride
            val pixelStride = yPlane.pixelStride
            val w = image.width
            val h = image.height

            fun sampleUpright(ux: Float, uy: Float): Float {
                // Map upright normalized → sensor pixel.
                val (sx, sy) = MatrixUtils.uprightNormalizedToSensor(ux, uy, rotationDegrees)
                val px = (sx * (w - 1)).toInt().coerceIn(0, w - 1)
                val py = (sy * (h - 1)).toInt().coerceIn(0, h - 1)
                val index = py * rowStride + px * pixelStride
                if (index < 0 || index >= yBuf.capacity()) return 0.5f
                return (yBuf.get(index).toInt() and 0xff) / 255f
            }

            // Prefer last MediaPipe landmark positions (upright); else ballpark priors.
            val lm = lastNormalized
            fun ux(i: Int, fallback: Float) = lm.getOrNull(i)?.x ?: fallback
            fun uy(i: Int, fallback: Float) = lm.getOrNull(i)?.y ?: fallback
            val left = sampleUpright(ux(CHEEK_LEFT, 0.35f), uy(CHEEK_LEFT, 0.55f))
            val right = sampleUpright(ux(CHEEK_RIGHT, 0.65f), uy(CHEEK_RIGHT, 0.55f))
            val forehead = sampleUpright(ux(10, 0.50f), uy(10, 0.28f))
            val nose = sampleUpright(ux(1, 0.50f), uy(1, 0.48f))
            // UV chroma (YUV_420_888). Plane1=U, plane2=V even when pixelStride=2 interleaved.
            fun planeSample(plane: androidx.camera.core.ImageProxy.PlaneProxy, x: Int, y: Int): Float {
                val buf = plane.buffer.duplicate()
                val idx = y * plane.rowStride + x * plane.pixelStride
                if (idx < 0 || idx >= buf.capacity()) return 0.5f
                return (buf.get(idx).toInt() and 0xff) / 255f
            }
            fun sampleUv(uxN: Float, uyN: Float): Pair<Float, Float> {
                if (image.planes.size < 3) return 0.5f to 0.5f
                val (sx, sy) = MatrixUtils.uprightNormalizedToSensor(uxN, uyN, rotationDegrees)
                val uPlane = image.planes[1]
                val vPlane = image.planes[2]
                val fullX = (sx * (w - 1)).toInt().coerceIn(0, w - 1)
                val fullY = (sy * (h - 1)).toInt().coerceIn(0, h - 1)
                // 4:2:0 chroma is subsampled; clamp to chroma plane extents.
                val chromaW = ((w + 1) / 2).coerceAtLeast(1)
                val chromaH = ((h + 1) / 2).coerceAtLeast(1)
                val cx = (fullX / 2).coerceIn(0, chromaW - 1)
                val cy = (fullY / 2).coerceIn(0, chromaH - 1)
                return planeSample(uPlane, cx, cy) to planeSample(vPlane, cx, cy)
            }
            fun yuvToRgb(y: Float, u: Float, v: Float, into: FloatArray) {
                val uf = u - 0.5f
                val vf = v - 0.5f
                into[0] = (y + 1.402f * vf).coerceIn(0f, 1f)
                into[1] = (y - 0.344136f * uf - 0.714136f * vf).coerceIn(0f, 1f)
                into[2] = (y + 1.772f * uf).coerceIn(0f, 1f)
            }
            val (lu, lv) = sampleUv(ux(CHEEK_LEFT, 0.35f), uy(CHEEK_LEFT, 0.55f))
            val (ru, rv) = sampleUv(ux(CHEEK_RIGHT, 0.65f), uy(CHEEK_RIGHT, 0.55f))
            synchronized(cheekLock) {
                yuvToRgb(left, lu, lv, leftCheekScratch)
                yuvToRgb(right, ru, rv, rightCheekScratch)
                lastFaceLuma = ((left + right + forehead + nose) * 0.25f).coerceIn(0.05f, 0.95f)
            }
        } catch (_: Exception) {
            // Keep previous samples on failure.
        }
    }

    /** Upper-cheek UV outside the under-eye mask (texture / GL space). */
    private fun nudgeCheekUv(texture: List<LandmarkPoint>, index: Int, left: Boolean): LandmarkPoint {
        val p = texture.getOrNull(index)
            ?: return if (left) LandmarkPoint(0.35f, 0.45f) else LandmarkPoint(0.65f, 0.45f)
        // Derive "outward" from the mapped face itself. Anatomical left/right swap screen sides
        // in mirror mode, so a fixed sign would move one cheek sample toward the nose.
        val centerX = texture.getOrNull(1)?.x ?: 0.5f
        val outward = if (p.x < centerX) -0.025f else 0.025f
        return LandmarkPoint(
            (p.x + outward).coerceIn(0f, 1f),
            (p.y - 0.03f).coerceIn(0f, 1f),
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy, maxSide: Int): Bitmap? {
        return try {
            val width = image.width
            val height = image.height
            val yPlane = image.planes[0]
            val uPlane = image.planes[1]
            val vPlane = image.planes[2]
            val need = width * height + 2 * ((width + 1) / 2) * ((height + 1) / 2)
            val nv21 = nv21Scratch?.takeIf { it.size >= need } ?: ByteArray(need).also { nv21Scratch = it }

            val yBuf = yPlane.buffer.duplicate()
            val yRow = yPlane.rowStride
            val yPix = yPlane.pixelStride
            var dst = 0
            for (row in 0 until height) {
                val rowStart = row * yRow
                if (yPix == 1) {
                    yBuf.position(rowStart)
                    yBuf.get(nv21, dst, width)
                    dst += width
                } else {
                    for (col in 0 until width) {
                        nv21[dst++] = yBuf.get(rowStart + col * yPix)
                    }
                }
            }

            val chromaW = (width + 1) / 2
            val chromaH = (height + 1) / 2
            val uBuf = uPlane.buffer.duplicate()
            val vBuf = vPlane.buffer.duplicate()
            for (row in 0 until chromaH) {
                for (col in 0 until chromaW) {
                    val uIdx = row * uPlane.rowStride + col * uPlane.pixelStride
                    val vIdx = row * vPlane.rowStride + col * vPlane.pixelStride
                    val u = if (uIdx < uBuf.capacity()) uBuf.get(uIdx) else 0x80.toByte()
                    val v = if (vIdx < vBuf.capacity()) vBuf.get(vIdx) else 0x80.toByte()
                    nv21[dst++] = v
                    nv21[dst++] = u
                }
            }

            val yuv = android.graphics.YuvImage(
                nv21, android.graphics.ImageFormat.NV21, width, height, null,
            )
            jpegScratch.reset()
            yuv.compressToJpeg(android.graphics.Rect(0, 0, width, height), 70, jpegScratch)
            val bytes = jpegScratch.toByteArray()
            val full = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val scale = maxSide.toFloat() / max(width, height)
            val bw = max(1, (width * scale.coerceAtMost(1f)).toInt())
            val bh = max(1, (height * scale.coerceAtMost(1f)).toInt())
            val result = if (full.width == bw && full.height == bh) {
                full
            } else {
                Bitmap.createScaledBitmap(full, bw, bh, true).also { scaled ->
                    if (scaled !== full) full.recycle()
                }
            }
            val previous = scaledScratch
            if (previous != null && previous !== result && !previous.isRecycled) previous.recycle()
            scaledScratch = result
            result
        } catch (t: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "YUV convert failed", t)
            null
        }
    }


    /** Refresh preview geometry from live mapper without touching analysis rotation/size. */
    private fun cfgWithLivePreview(cfg: FaceCoordinateMapper.Config): FaceCoordinateMapper.Config {
        val live = mapper.config
        return cfg.copy(
            previewWidth = live.previewWidth,
            previewHeight = live.previewHeight,
            mirrorFront = live.mirrorFront,
            cropLeft = live.cropLeft,
            cropTop = live.cropTop,
            cropWidth = live.cropWidth,
            cropHeight = live.cropHeight,
        )
    }

    companion object {
        private const val TAG = "FaceLandmarkerEngine"
        const val MODEL_ASSET = "face_landmarker.task"
        // MediaPipe face mesh cheek / mid-face landmarks.
        private const val CHEEK_LEFT = 50
        private const val CHEEK_RIGHT = 280
    }
}
