package com.beautymirror.app.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.beautymirror.app.R
import com.beautymirror.app.camera.CameraController
import com.beautymirror.app.camera.CameraState
import com.beautymirror.app.camera.CaptureSizeCalculator
import com.beautymirror.app.camera.ProcessedCaptureController
import com.beautymirror.app.rendering.BeautyRenderer
import com.beautymirror.app.rendering.FrameTimingCollector
import com.beautymirror.app.ota.OtaController
import com.beautymirror.app.settings.AdaptivePerformanceState
import com.beautymirror.app.settings.BeautySettings
import com.beautymirror.app.settings.QualityLevel
import com.beautymirror.app.settings.QuickFixSession
import com.beautymirror.app.settings.ReflectionScene
import com.beautymirror.app.tracking.FaceTrackingResult
import com.beautymirror.app.ui.theme.BmAccent
import com.beautymirror.app.ui.theme.BmBg
import com.beautymirror.app.ui.theme.BmDanger
import com.beautymirror.app.ui.theme.BmSurface
import com.beautymirror.app.ui.theme.BmSurfaceStrong
import com.beautymirror.app.ui.theme.BmText
import com.beautymirror.app.ui.theme.BmTextMuted
import kotlinx.coroutines.launch

@Composable
fun MirrorScreen(
    cameraController: CameraController,
    renderer: BeautyRenderer,
    captureController: ProcessedCaptureController,
    settings: BeautySettings,
    onSettingsChange: (BeautySettings) -> Unit,
    tracking: FaceTrackingResult,
    timing: FrameTimingCollector.Snapshot?,
    runtimeQuality: QualityLevel,
    performanceState: AdaptivePerformanceState,
    revealProgress: Float = 0f,
    pipelineReady: Boolean = true,
    statusMessage: String? = null,
    startWithChromeHidden: Boolean = false,
    otaController: OtaController? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val cameraState by cameraController.state.collectAsState()
    var holdingBeforeAfter by remember { mutableStateOf(false) }
    var pendingCapture by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var chromeVisible by rememberSaveable { mutableStateOf(!startWithChromeHidden) }
    var controlsVisible by rememberSaveable { mutableStateOf(false) }
    var lakePanelVisible by rememberSaveable { mutableStateOf(false) }
    var studioDockTop by rememberSaveable { mutableStateOf(false) }
    var activeFocus by remember { mutableStateOf(BeautyFocus.OVERVIEW) }
    // Lives above BeautyControls so Done (AnimatedVisibility exit) does not wipe one-tap toggles.
    val quickFixSession = remember { QuickFixSession() }

    fun commitPondExperience() {
        holdingBeforeAfter = false
        controlsVisible = false
        lakePanelVisible = false
        activeFocus = BeautyFocus.OVERVIEW
        val committed = settings.copy(
            reflectionScene = ReflectionScene.DARK_LAKE,
            showBeforeAfter = false,
        ).clamped()
        if (committed != settings) onSettingsChange(committed)
        renderer.restartVisitorReveal()
        // Done returns directly to the artwork, not to another layer of application chrome.
        chromeVisible = false
    }

    BackHandler(enabled = controlsVisible || lakePanelVisible || !chromeVisible) {
        when {
            controlsVisible || lakePanelVisible -> commitPondExperience()
            !chromeVisible -> chromeVisible = true
        }
    }

    val needsLegacyStorage = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
    fun hasLegacyStorage(): Boolean =
        !needsLegacyStorage ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED

    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            pendingCapture = true
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.storage_permission_needed),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    LaunchedEffect(lifecycleOwner, pipelineReady) {
        if (pipelineReady) cameraController.start(lifecycleOwner)
    }

    LaunchedEffect(holdingBeforeAfter) {
        renderer.compareHold = holdingBeforeAfter
    }

    LaunchedEffect(chromeVisible) {
        if (!chromeVisible) {
            controlsVisible = false
            lakePanelVisible = false
            holdingBeforeAfter = false
        }
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (chromeVisible) {
                    show(WindowInsetsCompat.Type.systemBars())
                } else {
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }

    DisposableEffect(activity, view) {
        onDispose {
            activity?.window?.let { window ->
                WindowCompat.getInsetsController(window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    suspend fun doCapture() {
        if (isCapturing) return
        isCapturing = true
        try {
            val captureSize = CaptureSizeCalculator.fitWithin(
                cameraController.displayWidth,
                cameraController.displayHeight,
                maxLongEdge = 1920,
            )
            val result = captureController.capture(captureSize.width, captureSize.height)
            val message = when (result) {
                is ProcessedCaptureController.CaptureResult.Success ->
                    context.getString(R.string.capture_saved)
                is ProcessedCaptureController.CaptureResult.Failure ->
                    result.message.ifBlank { context.getString(R.string.capture_failed) }
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } finally {
            isCapturing = false
        }
    }

    LaunchedEffect(pendingCapture) {
        if (pendingCapture) {
            pendingCapture = false
            doCapture()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BmBg),
    ) {
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).also { surfaceView ->
                    surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) = Unit

                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int,
                        ) {
                            cameraController.bindDisplaySurface(holder.surface, width, height)
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            cameraController.unbindDisplaySurface()
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x55000000),
                                Color.Transparent,
                                Color.Transparent,
                                Color(0x66000000),
                            ),
                        ),
                    ),
            )
        }

        DebugOverlay(
            tracking = tracking,
            timing = timing,
            enabled = chromeVisible && settings.debugOverlay,
            topPanelOffset = if (statusMessage != null) 150.dp else 88.dp,
        )

        AnimatedVisibility(
            visible = settings.reflectionScene == ReflectionScene.DARK_LAKE &&
                !tracking.isValid &&
                !chromeVisible &&
                !controlsVisible &&
                !lakePanelVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            PondWaitingOverlay(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 30.dp),
            )
        }

        AnimatedVisibility(
            visible = settings.reflectionScene == ReflectionScene.DARK_LAKE &&
                tracking.isValid &&
                revealProgress in 0.001f..0.995f &&
                !controlsVisible &&
                !lakePanelVisible &&
                !settings.debugOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            PondRevealOverlay(
                progress = revealProgress,
                durationSeconds = settings.revealDurationSeconds,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = if (chromeVisible) 116.dp else 24.dp,
                    ),
            )
        }

        // Dismiss layer under chrome controls — tap empty preview closes studio / lake panel.
        if (chromeVisible && (controlsVisible || lakePanelVisible)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.06f))
                    .clickable(
                        onClickLabel = context.getString(R.string.done),
                        onClick = { commitPondExperience() },
                    ),
            )
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            TopChrome(
                tracking = tracking,
                timing = timing,
                runtimeQuality = runtimeQuality,
                performanceState = performanceState,
                reflectionScene = settings.reflectionScene,
                onSwitchCamera = { scope.launch { cameraController.switchCamera(lifecycleOwner) } },
                onHide = {
                    if (controlsVisible || lakePanelVisible) commitPondExperience() else chromeVisible = false
                },
            )
        }

        FaceFocusOverlay(
            tracking = tracking,
            focus = activeFocus,
            visible = chromeVisible && controlsVisible,
        )

        AnimatedVisibility(
            visible = chromeVisible && controlsVisible,
            enter = fadeIn() + slideInVertically(
                initialOffsetY = { if (studioDockTop) -it / 2 else it / 2 },
            ),
            exit = fadeOut() + slideOutVertically(
                targetOffsetY = { if (studioDockTop) -it / 2 else it / 2 },
            ),
            modifier = Modifier.align(if (studioDockTop) Alignment.TopCenter else Alignment.BottomCenter),
        ) {
            BeautyControls(
                settings = settings,
                runtimeQuality = runtimeQuality,
                performanceState = performanceState,
                timing = timing,
                onChange = { onSettingsChange(it.copy(showBeforeAfter = false)) },
                onDismiss = { commitPondExperience() },
                onFocusChange = { activeFocus = it },
                otaController = otaController,
                quickFixSession = quickFixSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (studioDockTop) {
                            Modifier
                                .statusBarsPadding()
                                .padding(top = 76.dp, start = 12.dp, end = 12.dp)
                        } else {
                            Modifier
                                .navigationBarsPadding()
                                .padding(bottom = 112.dp, start = 12.dp, end = 12.dp)
                        },
                    ),
            )
        }

        AnimatedVisibility(
            visible = chromeVisible && lakePanelVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            LakeAdjustPanel(
                settings = settings,
                onChange = { onSettingsChange(it.copy(showBeforeAfter = false)) },
                onDismiss = { commitPondExperience() },
                onTurnOff = {
                    lakePanelVisible = false
                    onSettingsChange(settings.copy(reflectionScene = ReflectionScene.MIRROR))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = 112.dp),
            )
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            CaptureDock(
                isCapturing = isCapturing,
                pipelineReady = pipelineReady,
                controlsVisible = controlsVisible,
                holdingBeforeAfter = holdingBeforeAfter,
                lakeActive = settings.reflectionScene == ReflectionScene.DARK_LAKE,
                onToggleLake = {
                    controlsVisible = false
                    if (settings.reflectionScene != ReflectionScene.DARK_LAKE) {
                        onSettingsChange(settings.copy(reflectionScene = ReflectionScene.DARK_LAKE))
                    }
                    lakePanelVisible = !lakePanelVisible
                },
                onToggleControls = {
                    val opening = !controlsVisible
                    lakePanelVisible = false
                    if (opening) {
                        // Dock controls opposite the current face position and keep them stable
                        // for the whole editing session so the panel never jumps while posing.
                        studioDockTop = tracking.isValid && tracking.bounds.centerY > 0.54f
                        activeFocus = BeautyFocus.OVERVIEW
                    }
                    controlsVisible = opening
                },
                onCapture = {
                    scope.launch {
                        if (!hasLegacyStorage()) {
                            storageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            doCapture()
                        }
                    }
                },
                onBeforePress = { pressed -> holdingBeforeAfter = pressed },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }

        if (!chromeVisible) {
            // In exhibition mode the artwork stays clean: a visitor tap cannot accidentally expose
            // admin controls. Curators can long-press anywhere; normal app mode keeps the reveal icon.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(startWithChromeHidden) {
                        detectTapGestures(
                            onLongPress = { chromeVisible = true },
                            onTap = { if (!startWithChromeHidden) chromeVisible = true },
                        )
                    },
            )
            if (!startWithChromeHidden) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp),
                    shape = CircleShape,
                    color = BmSurface.copy(alpha = 0.52f),
                ) {
                    IconButton(
                        onClick = { chromeVisible = true },
                        modifier = Modifier.testTag("show_chrome"),
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = context.getString(R.string.show_overlay),
                            tint = BmText.copy(alpha = 0.78f),
                        )
                    }
                }
            }
        }

        if (statusMessage != null && chromeVisible) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 86.dp, start = 18.dp, end = 18.dp),
                shape = RoundedCornerShape(16.dp),
                color = BmSurfaceStrong,
            ) {
                Text(
                    text = statusMessage,
                    color = BmDanger,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        when {
            !pipelineReady -> LoadingState(
                message = statusMessage ?: context.getString(R.string.starting_camera),
                isError = statusMessage != null,
                modifier = Modifier.align(Alignment.Center),
            )

            cameraState is CameraState.Error -> {
                val error = cameraState as CameraState.Error
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = BmSurfaceStrong,
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = error.message, color = BmDanger)
                        TextButton(
                            onClick = { scope.launch { cameraController.start(lifecycleOwner) } },
                        ) {
                            Text(context.getString(R.string.retry), color = BmAccent)
                        }
                    }
                }
            }

            cameraState is CameraState.Starting -> LoadingState(
                message = context.getString(R.string.opening_camera),
                isError = false,
                modifier = Modifier.align(Alignment.Center),
            )

            else -> Unit
        }
    }
}

@Composable
private fun TopChrome(
    tracking: FaceTrackingResult,
    timing: FrameTimingCollector.Snapshot?,
    runtimeQuality: QualityLevel,
    performanceState: AdaptivePerformanceState,
    reflectionScene: ReflectionScene,
    onSwitchCamera: () -> Unit,
    onHide: () -> Unit,
) {
    val context = LocalContext.current
    val fps = timing?.cameraFps ?: 0.0
    val cameraLimited = performanceState.cameraLimited
    val fpsHealthy = fps <= 0.0 || fps >= 29.0 || cameraLimited
    val protecting = performanceState.protecting && !cameraLimited
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = BmSurface,
        ) {
            Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)) {
                Text(
                    text = context.getString(R.string.app_name),
                    color = BmText,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = buildString {
                        append(
                            if (tracking.isValid) {
                                context.getString(R.string.face_locked)
                            } else {
                                context.getString(R.string.finding_face)
                            },
                        )
                        if (reflectionScene == ReflectionScene.DARK_LAKE) {
                            append(" · ")
                            append(context.getString(R.string.dark_lake).uppercase())
                        }
                    },
                    color = if (tracking.isValid) BmAccent else BmTextMuted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BmSurface,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = if (fps > 0.0) "%.0f FPS".format(fps) else context.getString(R.string.fps_measuring),
                        color = when {
                            cameraLimited -> BmTextMuted
                            !fpsHealthy -> BmDanger
                            protecting -> BmAccent
                            else -> BmText
                        },
                        fontSize = 12.sp,
                    )
                    Text(
                        text = stringResource(runtimeQuality.labelRes()),
                        color = BmTextMuted,
                        fontSize = 9.sp,
                    )
                }
            }
            Surface(shape = CircleShape, color = BmSurface) {
                IconButton(
                    onClick = onSwitchCamera,
                    modifier = Modifier.testTag("switch_camera"),
                ) {
                    Icon(
                        Icons.Default.Cameraswitch,
                        contentDescription = context.getString(R.string.switch_camera),
                        tint = BmText,
                    )
                }
            }
            Surface(shape = CircleShape, color = BmSurface) {
                IconButton(
                    onClick = onHide,
                    modifier = Modifier.testTag("hide_chrome"),
                ) {
                    Icon(
                        Icons.Default.VisibilityOff,
                        contentDescription = context.getString(R.string.hide_overlay),
                        tint = BmText,
                    )
                }
            }
        }
    }
}

@Composable
private fun PondWaitingOverlay(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Canvas(modifier = Modifier.size(62.dp)) {
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            drawCircle(
                color = Color(0xB8DDF4FF),
                radius = size.minDimension * 0.10f,
                center = center,
            )
            drawCircle(
                color = Color(0x88B9E6FF),
                radius = size.minDimension * 0.25f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx()),
            )
            drawCircle(
                color = Color(0x66A0D9F8),
                radius = size.minDimension * 0.43f,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.pond_waiting_title),
                color = Color(0xFFF1FAFF),
                fontSize = 13.sp,
            )
            Text(
                stringResource(R.string.pond_waiting_subtitle),
                color = Color(0xC9DDF2FF),
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun PondRevealOverlay(
    progress: Float,
    durationSeconds: Float,
    modifier: Modifier = Modifier,
) {
    val safe = progress.coerceIn(0f, 1f)
    val remaining = (durationSeconds * (1f - safe)).coerceAtLeast(0f)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color(0xA1122B42),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                progress = safe,
                modifier = Modifier.size(30.dp),
                color = Color(0xFFE3F6FF),
                trackColor = Color(0x5579C8F2),
                strokeWidth = 2.dp,
            )
            Column {
                Text(
                    stringResource(R.string.pond_keep_looking),
                    color = Color(0xFFE8EAE5),
                    fontSize = 12.sp,
                )
                Text(
                    stringResource(R.string.pond_reveal_remaining, remaining),
                    color = Color(0xA8E8EAE5),
                    fontSize = 9.sp,
                )
            }
        }
    }
}

@Composable
private fun LakeAdjustPanel(
    settings: BeautySettings,
    onChange: (BeautySettings) -> Unit,
    onDismiss: () -> Unit,
    onTurnOff: () -> Unit,
    modifier: Modifier = Modifier,
) {
    fun mood(
        intensity: Float,
        motion: Float,
        darkness: Float,
        clarity: Float,
        cameraBlend: Float,
        deformation: Float,
        swirl: Float,
    ) {
        onChange(
            settings.copy(
                reflectionScene = ReflectionScene.DARK_LAKE,
                lakeIntensity = intensity,
                lakeMotion = motion,
                lakeDarkness = darkness,
                lakeFaceClarity = clarity,
                lakeCameraBlend = cameraBlend,
                lakeDeformation = deformation,
                lakeSwirl = swirl,
            ).clamped(),
        )
    }

    Surface(
        modifier = modifier
            .clickable(onClick = {})
            .testTag("lake_adjust_panel"),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xF0182C3D),
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 590.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0x45E1E5DC)),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(stringResource(R.string.dark_lake), color = BmText, fontSize = 17.sp)
                    Text(
                        stringResource(R.string.pond_new_face_restart),
                        color = BmTextMuted,
                        fontSize = 10.sp,
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.defaultMinSize(minHeight = 42.dp),
                ) {
                    Text(stringResource(R.string.return_to_pond), color = BmAccent)
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0x66426C88),
            ) {
                Text(
                    stringResource(R.string.lake_scene_hint),
                    color = Color(0xFFD8DDD5),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }

            Text(stringResource(R.string.scene_moods), color = BmText, fontSize = 12.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                PondMoodButton(
                    title = stringResource(R.string.scene_mood_sky),
                    active = settings.lakeDeformation in 0.16f..0.29f,
                    modifier = Modifier.weight(1f),
                ) { mood(0.88f, 0.42f, 0.18f, 0.94f, 0.72f, 0.24f, 0.82f) }
                PondMoodButton(
                    title = stringResource(R.string.scene_mood_silk),
                    active = settings.lakeDeformation < 0.16f,
                    modifier = Modifier.weight(1f),
                ) { mood(0.82f, 0.27f, 0.12f, 0.98f, 0.78f, 0.10f, 0.62f) }
                PondMoodButton(
                    title = stringResource(R.string.scene_mood_fluid),
                    active = settings.lakeDeformation >= 0.34f,
                    modifier = Modifier.weight(1f),
                ) { mood(0.92f, 0.56f, 0.22f, 0.90f, 0.70f, 0.42f, 0.94f) }
            }

            LakeDurationSlider(
                seconds = settings.revealDurationSeconds,
                onValue = { onChange(settings.copy(revealDurationSeconds = it).clamped()) },
            )
            LakeSliderRow(
                title = stringResource(R.string.lake_intensity),
                value = settings.lakeIntensity,
                testTag = "popup_lake_intensity",
            ) { onChange(settings.copy(lakeIntensity = it).clamped()) }
            LakeSliderRow(
                title = stringResource(R.string.lake_camera_blend),
                value = settings.lakeCameraBlend,
                testTag = "popup_lake_camera_blend",
            ) { onChange(settings.copy(lakeCameraBlend = it).clamped()) }
            LakeSliderRow(
                title = stringResource(R.string.lake_face_clarity),
                value = settings.lakeFaceClarity,
                testTag = "popup_lake_clarity",
            ) { onChange(settings.copy(lakeFaceClarity = it).clamped()) }
            LakeSliderRow(
                title = stringResource(R.string.lake_deformation),
                value = settings.lakeDeformation,
                testTag = "popup_lake_deformation",
            ) { onChange(settings.copy(lakeDeformation = it).clamped()) }
            LakeSliderRow(
                title = stringResource(R.string.lake_swirl),
                value = settings.lakeSwirl,
                testTag = "popup_lake_swirl",
            ) { onChange(settings.copy(lakeSwirl = it).clamped()) }
            LakeSliderRow(
                title = stringResource(R.string.lake_motion),
                value = settings.lakeMotion,
                testTag = "popup_lake_motion",
            ) { onChange(settings.copy(lakeMotion = it).clamped()) }
            LakeSliderRow(
                title = stringResource(R.string.lake_darkness),
                value = settings.lakeDarkness,
                testTag = "popup_lake_depth",
            ) { onChange(settings.copy(lakeDarkness = it).clamped()) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onTurnOff,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 42.dp),
                ) {
                    Text(stringResource(R.string.off), color = BmTextMuted)
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(2f)
                        .defaultMinSize(minHeight = 42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x335DB8EA)),
                ) {
                    Text(stringResource(R.string.return_to_pond), color = BmText)
                }
            }
        }
    }
}

@Composable
private fun PondMoodButton(
    title: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (active) Color(0xFFD8DED5) else Color(0x3A758078),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                title,
                color = if (active) Color(0xFF172019) else Color(0xFFD9DED8),
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun LakeSliderRow(
    title: String,
    value: Float,
    testTag: String,
    onValue: (Float) -> Unit,
) {
    val safe = value.coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = BmText, fontSize = 12.sp)
            Text("${(safe * 100).toInt()}%", color = BmAccent, fontSize = 10.sp)
        }
        Slider(
            value = safe,
            onValueChange = onValue,
            modifier = Modifier
                .height(36.dp)
                .testTag(testTag),
            colors = SliderDefaults.colors(
                thumbColor = BmAccent,
                activeTrackColor = BmAccent,
                inactiveTrackColor = BmTextMuted.copy(alpha = 0.18f),
            ),
        )
    }
}

@Composable
private fun LakeDurationSlider(
    seconds: Float,
    onValue: (Float) -> Unit,
) {
    val minSeconds = 3f
    val maxSeconds = 30f
    val safe = seconds.coerceIn(minSeconds, maxSeconds)
    val normalized = (safe - minSeconds) / (maxSeconds - minSeconds)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.transformation_duration), color = BmText, fontSize = 12.sp)
            Text(stringResource(R.string.seconds_value, safe), color = BmAccent, fontSize = 10.sp)
        }
        Slider(
            value = normalized,
            onValueChange = { onValue(minSeconds + it * (maxSeconds - minSeconds)) },
            modifier = Modifier
                .height(36.dp)
                .testTag("popup_reveal_duration"),
            colors = SliderDefaults.colors(
                thumbColor = BmAccent,
                activeTrackColor = BmAccent,
                inactiveTrackColor = BmTextMuted.copy(alpha = 0.18f),
            ),
        )
    }
}

@Composable
private fun CaptureDock(
    isCapturing: Boolean,
    pipelineReady: Boolean,
    controlsVisible: Boolean,
    holdingBeforeAfter: Boolean,
    lakeActive: Boolean,
    onToggleLake: () -> Unit,
    onToggleControls: () -> Unit,
    onCapture: () -> Unit,
    onBeforePress: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        color = BmSurfaceStrong,
        shadowElevation = 12.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .padding(horizontal = 8.dp),
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DockAction(
                    icon = Icons.Default.WaterDrop,
                    label = context.getString(R.string.lake),
                    description = context.getString(R.string.toggle_lake),
                    active = lakeActive,
                    enabled = pipelineReady,
                    testTag = "dock_lake",
                    onClick = onToggleLake,
                )
                DockAction(
                    icon = Icons.Default.Tune,
                    label = context.getString(R.string.studio),
                    description = context.getString(R.string.toggle_controls),
                    active = controlsVisible,
                    testTag = "dock_studio",
                    onClick = onToggleControls,
                )
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(BmAccent.copy(alpha = 0.22f))
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (isCapturing) BmTextMuted else Color.White)
                        .semantics {
                            role = Role.Button
                            contentDescription = context.getString(R.string.capture_photo)
                        }
                        .clickable(enabled = !isCapturing && pipelineReady, onClick = onCapture)
                        .testTag("capture"),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = BmAccent,
                            strokeWidth = 3.dp,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(58.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = context.getString(R.string.hold_for_before_after)
                    }
                    .testTag("compare")
                    .pointerInput(onBeforePress) {
                        detectTapGestures(
                            onPress = {
                                onBeforePress(true)
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    onBeforePress(false)
                                }
                            },
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (holdingBeforeAfter) BmAccent else BmTextMuted.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Compare,
                        contentDescription = null,
                        tint = if (holdingBeforeAfter) BmBg else BmText,
                    )
                }
                Text(
                    context.getString(R.string.compare),
                    color = if (holdingBeforeAfter) BmAccent else BmTextMuted,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

@Composable
private fun DockAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    active: Boolean = false,
    enabled: Boolean = true,
    testTag: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (active) BmAccent else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = when {
                    !enabled -> BmTextMuted.copy(alpha = 0.40f)
                    active -> BmBg
                    else -> BmText
                },
            )
        }
        Text(label, color = if (active) BmAccent else BmTextMuted, fontSize = 9.sp)
    }
}

@Composable
private fun LoadingState(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp),
        shape = RoundedCornerShape(22.dp),
        color = BmSurfaceStrong,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isError) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = BmAccent,
                    strokeWidth = 2.dp,
                )
            }
            Text(message, color = if (isError) BmDanger else BmText)
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
