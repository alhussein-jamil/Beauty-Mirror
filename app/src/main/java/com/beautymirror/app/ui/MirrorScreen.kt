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
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Tune
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
    var studioDockTop by rememberSaveable { mutableStateOf(false) }
    var activeFocus by remember { mutableStateOf(BeautyFocus.OVERVIEW) }
    // Lives above BeautyControls so Done (AnimatedVisibility exit) does not wipe one-tap toggles.
    val quickFixSession = remember { QuickFixSession() }

    fun commitPondExperience() {
        holdingBeforeAfter = false
        controlsVisible = false
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

    BackHandler(enabled = controlsVisible || !chromeVisible) {
        when {
            controlsVisible -> commitPondExperience()
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



        // Empty-preview tap toggles chrome (and closes studio if open).
        if (chromeVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        onClickLabel = context.getString(R.string.hide_overlay),
                        onClick = {
                            holdingBeforeAfter = false
                            controlsVisible = false
                            activeFocus = BeautyFocus.OVERVIEW
                            chromeVisible = false
                        },
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
                performanceState = performanceState,
                onSwitchCamera = { scope.launch { cameraController.switchCamera(lifecycleOwner) } },
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
                onToggleControls = {
                    val opening = !controlsVisible
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
            // Artwork only — tap anywhere restores chrome.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        onClickLabel = context.getString(R.string.show_overlay),
                        onClick = { chromeVisible = true },
                    ),
            )
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
    performanceState: AdaptivePerformanceState,
    onSwitchCamera: () -> Unit,
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

        Surface(
            shape = CircleShape,
            color = BmSurface,
            modifier = Modifier.testTag("face_indicator"),
        ) {
            Icon(
                Icons.Default.Face,
                contentDescription = context.getString(
                    if (tracking.isValid) R.string.face_locked else R.string.finding_face,
                ),
                tint = if (tracking.isValid) BmAccent else BmTextMuted,
                modifier = Modifier
                    .padding(10.dp)
                    .size(22.dp),
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = BmSurface,
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
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun CaptureDock(
    isCapturing: Boolean,
    pipelineReady: Boolean,
    controlsVisible: Boolean,
    holdingBeforeAfter: Boolean,
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
