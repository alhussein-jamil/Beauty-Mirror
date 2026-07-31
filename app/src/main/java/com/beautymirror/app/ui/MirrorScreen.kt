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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    var lakePanelVisible by rememberSaveable { mutableStateOf(false) }
    var studioDockTop by rememberSaveable { mutableStateOf(false) }
    var activeFocus by remember { mutableStateOf(BeautyFocus.OVERVIEW) }

    BackHandler(enabled = controlsVisible || lakePanelVisible || !chromeVisible) {
        when {
            lakePanelVisible -> lakePanelVisible = false
            controlsVisible -> controlsVisible = false
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

        // Dismiss layer under chrome controls — tap empty preview closes studio / lake panel.
        if (chromeVisible && (controlsVisible || lakePanelVisible)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.06f))
                    .clickable(
                        onClickLabel = context.getString(R.string.done),
                        onClick = {
                            controlsVisible = false
                            lakePanelVisible = false
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
                runtimeQuality = runtimeQuality,
                performanceState = performanceState,
                reflectionScene = settings.reflectionScene,
                onSwitchCamera = { scope.launch { cameraController.switchCamera(lifecycleOwner) } },
                onHide = {
                    chromeVisible = false
                    lakePanelVisible = false
                    controlsVisible = false
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
                onDismiss = {
                    controlsVisible = false
                },
                onFocusChange = { activeFocus = it },
                otaController = otaController,
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
                onDismiss = { lakePanelVisible = false },
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
                    val lakeOn = settings.reflectionScene == ReflectionScene.DARK_LAKE
                    when {
                        !lakeOn -> {
                            controlsVisible = false
                            onSettingsChange(settings.copy(reflectionScene = ReflectionScene.DARK_LAKE))
                            lakePanelVisible = true
                        }
                        lakePanelVisible -> {
                            lakePanelVisible = false
                            onSettingsChange(settings.copy(reflectionScene = ReflectionScene.MIRROR))
                        }
                        else -> {
                            controlsVisible = false
                            lakePanelVisible = true
                        }
                    }
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { chromeVisible = true })
                    },
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp),
                shape = CircleShape,
                color = BmSurface,
            ) {
                IconButton(
                    onClick = { chromeVisible = true },
                    modifier = Modifier.testTag("show_chrome"),
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = context.getString(R.string.show_overlay),
                        tint = BmText,
                    )
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
private fun LakeAdjustPanel(
    settings: BeautySettings,
    onChange: (BeautySettings) -> Unit,
    onDismiss: () -> Unit,
    onTurnOff: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clickable(onClick = {})
            .testTag("lake_adjust_panel"),
        shape = RoundedCornerShape(24.dp),
        color = BmSurface,
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.dark_lake), color = BmText, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = onTurnOff,
                        modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                    ) {
                        Text(stringResource(R.string.off), color = BmTextMuted)
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                    ) {
                        Text(stringResource(R.string.done), color = BmAccent)
                    }
                }
            }
            LakeSliderRow(
                title = stringResource(R.string.lake_intensity),
                value = settings.lakeIntensity,
                testTag = "popup_lake_intensity",
            ) { onChange(settings.copy(lakeIntensity = it).clamped()) }
            LakeSliderRow(
                title = stringResource(R.string.lake_motion),
                value = settings.lakeMotion,
                testTag = "popup_lake_motion",
            ) { onChange(settings.copy(lakeMotion = it).clamped()) }
            LakeSliderRow(
                title = stringResource(R.string.lake_darkness),
                value = settings.lakeDarkness,
                testTag = "popup_lake_darkness",
            ) { onChange(settings.copy(lakeDarkness = it).clamped()) }
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
