package com.beautymirror.app.camera

import android.content.Context
import android.util.Log
import android.util.Range
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.beautymirror.app.BuildConfig
import com.beautymirror.app.rendering.BeautyRenderer
import com.beautymirror.app.settings.QualityLevel
import com.beautymirror.app.tracking.FaceCoordinateMapper
import com.beautymirror.app.tracking.FaceLandmarkerEngine
import com.beautymirror.app.util.AndroidDispatchers
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine

class CameraController(
    private val context: Context,
    private val dispatchers: AndroidDispatchers,
    private val renderer: BeautyRenderer,
    private val landmarker: FaceLandmarkerEngine,
    private val mapper: FaceCoordinateMapper,
) {
    private val _state = MutableStateFlow<CameraState>(CameraState.Idle)
    val state: StateFlow<CameraState> = _state.asStateFlow()

    private val providerRef = AtomicReference<ProcessCameraProvider?>(null)
    private val released = AtomicBoolean(false)
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var analysis: ImageAnalysis? = null
    @Volatile
    private var config = CameraConfiguration()
    private var lifecycleOwner: LifecycleOwner? = null
    @Volatile private var mirrorPreviewEnabled: Boolean = true
    private var previewWidth = 1080
    private var previewHeight = 1920
    private val bindMutex = Mutex()

    val isFront: Boolean get() = config.isFront
    val displayWidth: Int get() = previewWidth
    val displayHeight: Int get() = previewHeight

    suspend fun start(owner: LifecycleOwner, configuration: CameraConfiguration = config) {
        bindMutex.withLock {
            if (released.get()) return@withLock
            _state.value = CameraState.Starting
            try {
                val provider = obtainProvider()
                val selector = selectorFor(configuration)
                check(provider.hasCamera(selector)) {
                    if (configuration.isFront) "Front camera unavailable" else "Rear camera unavailable"
                }
                providerRef.set(provider)
                lifecycleOwner = owner
                config = configuration
                renderer.isFrontCamera = configuration.isFront
                renderer.mirrorPreviewEnabled = mirrorPreviewEnabled
                mapper.config = mapper.config.copy(
                    mirrorFront = configuration.isFront && mirrorPreviewEnabled,
                )
                landmarker.setQuality(configuration.qualityLevel)
                landmarker.onCameraChanged()
                bindUseCasesLocked(provider, owner, configuration, selector)
                _state.value = CameraState.Live
            } catch (t: Throwable) {
                if (BuildConfig.DEBUG) Log.e(TAG, "start failed", t)
                _state.value = CameraState.Error(t.message ?: "Camera start failed", t)
            }
        }
    }

    suspend fun switchCamera(owner: LifecycleOwner) {
        val nextFacing = if (config.isFront) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
        start(owner, config.copy(lensFacing = nextFacing))
    }

    suspend fun updateQuality(level: QualityLevel, rebindCamera: Boolean = true) {
        bindMutex.withLock {
            val prevH = config.qualityLevel.previewTargetHeight
            config = config.copy(qualityLevel = level)
            landmarker.setQuality(level)
            renderer.updateQuality(level)
            val needRebind = rebindCamera && level.previewTargetHeight != prevH
            if (!needRebind) return@withLock
            val owner = lifecycleOwner ?: return@withLock
            val provider = providerRef.get() ?: return@withLock
            try {
                landmarker.onCameraChanged()
                bindUseCasesLocked(provider, owner, config)
            } catch (t: Throwable) {
                if (BuildConfig.DEBUG) Log.w(TAG, "rebind quality failed", t)
            }
        }
    }

    /** Adaptive path: change analysis rate + GPU size without camera rebind flicker. */
    fun applyRuntimeQuality(level: QualityLevel) {
        config = config.copy(qualityLevel = level)
        landmarker.setQuality(level)
        renderer.updateQuality(level)
    }

    fun setMirrorPreview(enabled: Boolean) {
        mirrorPreviewEnabled = enabled
        renderer.mirrorPreviewEnabled = enabled
        mapper.config = mapper.config.copy(
            mirrorFront = config.isFront && enabled,
        )
    }

    fun bindDisplaySurface(surface: android.view.Surface, width: Int, height: Int) {
        previewWidth = width
        previewHeight = height
        mapper.config = mapper.config.copy(
            previewWidth = width,
            previewHeight = height,
            mirrorFront = config.isFront && mirrorPreviewEnabled,
        )
        renderer.setOutputSurface(surface, width, height)
    }

    fun unbindDisplaySurface() {
        renderer.setOutputSurface(null, 1, 1)
    }

    fun stop() {
        analysis?.clearAnalyzer()
        providerRef.get()?.unbindAll()
        camera = null
        preview = null
        analysis = null
        _state.value = CameraState.Idle
    }

    fun release() {
        if (!released.compareAndSet(false, true)) return
        stop()
        // MediaPipe was created and used on the analysis looper; close it there as well.
        dispatchers.runAnalysisBlocking { landmarker.release() }
        renderer.releaseSync()
        dispatchers.release()
        providerRef.set(null)
        lifecycleOwner = null
    }

    private fun bindUseCasesLocked(
        provider: ProcessCameraProvider,
        owner: LifecycleOwner,
        configuration: CameraConfiguration,
        selector: CameraSelector = selectorFor(configuration),
    ) {
        check(provider.hasCamera(selector)) {
            if (configuration.isFront) "Front camera unavailable" else "Rear camera unavailable"
        }
        analysis?.clearAnalyzer()
        provider.unbindAll()

        val targetHeight = configuration.qualityLevel.previewTargetHeight
        val analysisHeight = targetHeight.coerceAtMost(960)
        val previewSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size((targetHeight * 9 / 16), targetHeight),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                ),
            )
            .build()
        val analysisSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size((analysisHeight * 9 / 16).coerceAtLeast(360), analysisHeight),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                ),
            )
            .build()

        val previewUseCase = Preview.Builder()
            // Lock 30 FPS — matches quality target and stops 60 Hz double-render cost.
            .setTargetFrameRate(Range(30, 30))
            .setResolutionSelector(previewSelector)
            .build()
            .also { p ->
                p.setSurfaceProvider(
                    PreviewSurfaceProvider(
                        executor = dispatchers.camera,
                        surfaceProvider = { request ->
                            renderer.getCameraInputSurface(
                                request.resolution.width,
                                request.resolution.height,
                            )
                        },
                        onTransformationInfo = { request, info ->
                            val crop = info.cropRect
                            renderer.setCameraInputTransform(
                                crop.width().takeIf { it > 0 } ?: request.resolution.width,
                                crop.height().takeIf { it > 0 } ?: request.resolution.height,
                                info.rotationDegrees,
                                surfaceContainsCameraTransform = info.hasCameraTransform(),
                                cameraTransformRequestsMirror = info.isMirroring,
                            )
                        },
                        onFailure = { error ->
                            if (BuildConfig.DEBUG) Log.e(TAG, "preview surface failed", error)
                            _state.value = CameraState.Error(
                                error.message ?: "Camera preview surface failed",
                                error,
                            )
                        },
                    ),
                )
            }

        val analysisUseCase = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setResolutionSelector(analysisSelector)
            .build()
            .also { ia ->
                ia.setAnalyzer(dispatchers.analysisExecutor) { image ->
                    // Preview/mirror live in bindDisplaySurface / bindUseCases.
                    // Do not write mapper here — detectAsync snapshots config into inFlightConfig.
                    val accepted = landmarker.detectAsync(image, image.imageInfo.rotationDegrees)
                    // Throttling/busy are expected — do not count as dropped frames.
                    if (!accepted) image.close()
                }
            }

        camera = provider.bindToLifecycle(owner, selector, previewUseCase, analysisUseCase)
        preview = previewUseCase
        analysis = analysisUseCase
        renderer.isFrontCamera = configuration.isFront
        renderer.mirrorPreviewEnabled = mirrorPreviewEnabled
        mapper.config = mapper.config.copy(
            mirrorFront = configuration.isFront && mirrorPreviewEnabled,
            previewWidth = previewWidth,
            previewHeight = previewHeight,
        )
    }

    private fun selectorFor(configuration: CameraConfiguration): CameraSelector =
        CameraSelector.Builder()
            .requireLensFacing(configuration.lensFacing)
            .build()

    private suspend fun obtainProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    try {
                        cont.resume(future.get())
                    } catch (t: Throwable) {
                        cont.resumeWithException(t)
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        }

    companion object {
        private const val TAG = "CameraController"
    }
}
