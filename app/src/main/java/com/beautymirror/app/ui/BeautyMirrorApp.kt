package com.beautymirror.app.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.beautymirror.app.BuildConfig
import com.beautymirror.app.R
import com.beautymirror.app.camera.CameraController
import com.beautymirror.app.camera.ProcessedCaptureController
import com.beautymirror.app.rendering.BeautyRenderer
import com.beautymirror.app.rendering.FrameTimingCollector
import com.beautymirror.app.ota.OtaController
import com.beautymirror.app.settings.AdaptivePerformanceState
import com.beautymirror.app.settings.AdaptiveQualityController
import com.beautymirror.app.settings.BeautySettings
import com.beautymirror.app.settings.ReflectionScene
import com.beautymirror.app.settings.SettingsRepository
import com.beautymirror.app.tracking.FaceCoordinateMapper
import com.beautymirror.app.tracking.FaceLandmarkerEngine
import com.beautymirror.app.tracking.FaceTrackingResult
import com.beautymirror.app.ui.theme.BeautyTheme
import com.beautymirror.app.util.AndroidDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun BeautyMirrorApp(
    settingsRepository: SettingsRepository,
    launchExhibitionMode: Boolean = false,
) {
    BeautyTheme {
        val context = LocalContext.current
        val mainHandler = remember { Handler(Looper.getMainLooper()) }
        var permissionGranted by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED,
            )
        }
        var permanentlyDenied by remember { mutableStateOf(false) }
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            permissionGranted = granted
            if (!granted) {
                val activity = context as? Activity
                permanentlyDenied = activity != null &&
                    !activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            }
        }

        val permissionLifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(permissionLifecycleOwner, context) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA,
                    ) == PackageManager.PERMISSION_GRANTED
                    permissionGranted = granted
                    if (granted) permanentlyDenied = false
                }
            }
            permissionLifecycleOwner.lifecycle.addObserver(observer)
            onDispose { permissionLifecycleOwner.lifecycle.removeObserver(observer) }
        }

        if (!permissionGranted) {
            PermissionScreen(
                permanentlyDenied = permanentlyDenied,
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    )
                    context.startActivity(intent)
                },
            )
            return@BeautyTheme
        }

        val dispatchers = remember { AndroidDispatchers() }
        val mapper = remember { FaceCoordinateMapper() }
        val landmarker = remember { FaceLandmarkerEngine(context, mapper) }
        val renderer = remember { BeautyRenderer(context, dispatchers.glHandler) }
        val cameraController = remember {
            CameraController(context, dispatchers, renderer, landmarker, mapper)
        }
        val captureController = remember {
            ProcessedCaptureController(context, renderer, dispatchers.glHandler)
        }
        val otaController = remember { OtaController(context) }

        var settings by remember { mutableStateOf(BeautySettings.natural()) }
        var settingsHydrated by remember { mutableStateOf(false) }
        val stored by produceState<BeautySettings?>(initialValue = null, key1 = settingsRepository) {
            settingsRepository.settingsFlow.collect { value = it }
        }
        var launchOverrideApplied by remember { mutableStateOf(false) }
        // Hydrate once from DataStore. Later emissions are our own saves — applying them
        // would rewind in-flight slider edits when writes reorder.
        LaunchedEffect(stored, launchExhibitionMode) {
            val persisted = stored ?: return@LaunchedEffect
            if (launchExhibitionMode && !launchOverrideApplied) {
                launchOverrideApplied = true
                settings = BeautySettings.stage().copy(
                    debugOverlay = false,
                    mirrorPreview = true,
                    reflectionScene = ReflectionScene.DARK_LAKE,
                    lakeIntensity = 0.66f,
                    lakeMotion = 0.30f,
                    lakeDarkness = 0.50f,
                    lakeFaceClarity = 0.78f,
                )
                settingsHydrated = true
                return@LaunchedEffect
            }
            if (!settingsHydrated) {
                settings = persisted
                settingsHydrated = true
            }
        }

        var tracking by remember { mutableStateOf(FaceTrackingResult.empty()) }
        var timing by remember { mutableStateOf<FrameTimingCollector.Snapshot?>(null) }
        var pipelineReady by remember { mutableStateOf(false) }
        var landmarkerError by remember { mutableStateOf<String?>(null) }
        val adaptive = remember { AdaptiveQualityController() }
        // Runtime quality may drop under load; settings.qualityLevel stays the user ceiling.
        var runtimeQuality by remember { mutableStateOf(settings.qualityLevel) }
        var performanceState by remember { mutableStateOf(AdaptivePerformanceState.FULL) }

        // Apply restored settings immediately after renderer startup, not only after the first
        // user interaction. This also makes mirror mode deterministic on every launch.
        LaunchedEffect(settings, runtimeQuality, pipelineReady) {
            if (!pipelineReady) return@LaunchedEffect
            renderer.settings = settings.copy(
                showBeforeAfter = false,
                qualityLevel = runtimeQuality,
            )
            renderer.performanceState = performanceState
            cameraController.setMirrorPreview(settings.mirrorPreview)
        }

        // Debounced persist — never store transient compare (showBeforeAfter).
        // Exhibition / demo launches stay ephemeral so they do not overwrite the saved look.
        LaunchedEffect(settings, settingsHydrated, launchExhibitionMode) {
            if (!settingsHydrated || launchExhibitionMode) return@LaunchedEffect
            delay(280)
            settingsRepository.save(settings.copy(showBeforeAfter = false))
        }

        LaunchedEffect(Unit) {
            val rendererResult = runCatching {
                withContext(dispatchers.gl) { renderer.initializeOnGlThread() }
            }
            if (rendererResult.isFailure) {
                val failure = rendererResult.exceptionOrNull()
                if (BuildConfig.DEBUG) Log.e(TAG, "renderer init failed", failure)
                landmarkerError = failure?.message ?: context.getString(R.string.opengl_init_failed)
                return@LaunchedEffect
            }

            landmarker.setListener(object : FaceLandmarkerEngine.Listener {
                // Render path gets every result; Compose only needs ~12 Hz for chrome labels.
                private var lastUiPostMs = 0L
                private var lastUiValid: Boolean? = null

                override fun onTracking(result: FaceTrackingResult) {
                    renderer.markAnalysisFrame()
                    renderer.tracking = result
                    val now = SystemClock.elapsedRealtime()
                    val validityChanged = lastUiValid != result.isValid
                    if (!validityChanged && now - lastUiPostMs < 80L) return
                    lastUiPostMs = now
                    lastUiValid = result.isValid
                    mainHandler.post {
                        tracking = result
                        landmarkerError = null
                    }
                }

                override fun onError(message: String, error: Throwable?) {
                    if (BuildConfig.DEBUG) Log.w(TAG, message, error)
                    mainHandler.post { landmarkerError = message }
                }
            })

            val trackingReady = runCatching {
                withContext(dispatchers.analysis) { landmarker.initialize() }
            }.getOrElse { error ->
                if (BuildConfig.DEBUG) Log.e(TAG, "landmarker init failed", error)
                false
            }
            if (!trackingReady) {
                landmarkerError = context.getString(R.string.tracking_unavailable)
            }
            pipelineReady = true
        }

        LaunchedEffect(settings.qualityLevel, pipelineReady) {
            if (!pipelineReady) return@LaunchedEffect
            adaptive.setLevel(settings.qualityLevel)
            runtimeQuality = settings.qualityLevel
            performanceState = adaptive.performanceState()
            renderer.performanceState = performanceState
            landmarker.setQuality(settings.qualityLevel)
            cameraController.updateQuality(settings.qualityLevel)
        }

        LaunchedEffect(Unit) {
            while (true) {
                delay(500)
                val snap = renderer.timingSnapshot()
                timing = snap
                if (snap != null) {
                    val next = adaptive.evaluate(SystemClock.elapsedRealtime(), snap)
                    performanceState = adaptive.performanceState()
                    renderer.performanceState = performanceState
                    if (next != null && next != runtimeQuality) {
                        adaptive.applyAdaptive(next)
                        runtimeQuality = next
                        cameraController.applyRuntimeQuality(next)
                    }
                }
            }
        }

        DisposableEffect(otaController) {
            otaController.start()
            onDispose { otaController.stop() }
        }

        DisposableEffect(Unit) {
            onDispose {
                cameraController.release()
            }
        }

        MirrorScreen(
            cameraController = cameraController,
            renderer = renderer,
            captureController = captureController,
            settings = settings,
            onSettingsChange = { next ->
                settings = next.clamped().copy(showBeforeAfter = false)
            },
            tracking = tracking,
            timing = timing,
            runtimeQuality = runtimeQuality,
            performanceState = performanceState,
            pipelineReady = pipelineReady,
            statusMessage = landmarkerError,
            startWithChromeHidden = launchExhibitionMode,
            otaController = otaController,
        )
    }
}

private const val TAG = "BeautyMirrorApp"
