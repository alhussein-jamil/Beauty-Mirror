package com.beautymirror.app.rendering

import android.content.Context
import android.os.SystemClock
import com.beautymirror.app.masks.FaceMaskGenerator
import com.beautymirror.app.masks.MaskTextureRenderer
import com.beautymirror.app.rendering.passes.CameraInputPass
import com.beautymirror.app.rendering.passes.ColorCorrectionPass
import com.beautymirror.app.rendering.passes.DetailRestorationPass
import com.beautymirror.app.rendering.passes.FaceLightingPass
import com.beautymirror.app.rendering.passes.FaceWarpPass
import com.beautymirror.app.rendering.passes.FeatureEnhancementPass
import com.beautymirror.app.rendering.passes.FinalCompositePass
import com.beautymirror.app.rendering.passes.LakeReflectionPass
import com.beautymirror.app.rendering.passes.SkinSmoothingPass
import com.beautymirror.app.rendering.passes.UnderEyeCorrectionPass
import com.beautymirror.app.settings.AdaptivePerformanceState
import com.beautymirror.app.settings.BeautySettings
import com.beautymirror.app.settings.ReflectionScene
import com.beautymirror.app.settings.QualityLevel
import com.beautymirror.app.settings.SettingsInterpolator
import com.beautymirror.app.settings.VisitorRevealController
import com.beautymirror.app.settings.VisitorSessionDetector
import com.beautymirror.app.tracking.FaceTrackingResult
import com.beautymirror.app.util.MathUtils
import com.beautymirror.app.util.ResourceUtils
import kotlin.math.pow

/** Multipass, pose-aware beauty graph with ping-pong FBOs. */
class RenderGraph(context: Context) {
    private val vert = ResourceUtils.loadAssetText(context, "shaders/fullscreen.vert")
    private val maskVert = ResourceUtils.loadAssetText(context, "shaders/mask.vert")
    private val maskFrag = ResourceUtils.loadAssetText(context, "shaders/mask.frag")

    val cameraPass = CameraInputPass(vert, ResourceUtils.loadAssetText(context, "shaders/camera_oes.frag"))
    val warpPass = FaceWarpPass(vert, ResourceUtils.loadAssetText(context, "shaders/face_warp.frag"))
    val skinPass = SkinSmoothingPass(vert, ResourceUtils.loadAssetText(context, "shaders/skin_smoothing.frag"))
    val underEyePass = UnderEyeCorrectionPass(vert, ResourceUtils.loadAssetText(context, "shaders/under_eye_correction.frag"))
    val lightingPass = FaceLightingPass(vert, ResourceUtils.loadAssetText(context, "shaders/face_lighting.frag"))
    val detailPass = DetailRestorationPass(vert, ResourceUtils.loadAssetText(context, "shaders/detail_restoration.frag"))
    val featurePass = FeatureEnhancementPass(vert, ResourceUtils.loadAssetText(context, "shaders/feature_enhancement.frag"))
    val colorPass = ColorCorrectionPass(vert, ResourceUtils.loadAssetText(context, "shaders/color_correction.frag"))
    val lakePass = LakeReflectionPass(vert, ResourceUtils.loadAssetText(context, "shaders/lake_reflection.frag"))
    val compositePass = FinalCompositePass(vert, ResourceUtils.loadAssetText(context, "shaders/final_composite.frag"))

    val maskRenderer = MaskTextureRenderer(
        maskVert,
        maskFrag,
        vert,
        ResourceUtils.loadAssetText(context, "shaders/mask_blur.frag"),
    )
    private val maskGenerator = FaceMaskGenerator()
    private val settingsInterpolator = SettingsInterpolator()

    private val mesh = GlMesh()
    private val pingTex = GlTexture()
    private val pongTex = GlTexture()
    private val originalTex = GlTexture()
    private val pingFbo = GlFramebuffer()
    private val pongFbo = GlFramebuffer()
    private val originalFbo = GlFramebuffer()

    private var width = 0
    private var height = 0
    val timing = FrameTimingCollector()

    /** Latest UI/target settings. Displayed values are smoothed via [settingsInterpolator]. */
    @Volatile var settings: BeautySettings = BeautySettings.off()
        set(value) {
            field = value
            settingsInterpolator.setTarget(value)
        }

    @Volatile var tracking: FaceTrackingResult = FaceTrackingResult.empty()
    @Volatile var performanceState: AdaptivePerformanceState = AdaptivePerformanceState.FULL
    @Volatile var compareHold: Boolean = false

    private var maskSizeCached = 0
    private var cameraInputWidth = 1
    private var cameraInputHeight = 1
    private var cameraInputRotationDegrees = 0
    private var lastOutputFbo: GlFramebuffer? = null
    private var masksValid = false
    private var lastMaskTrackingTimestampNs = Long.MIN_VALUE
    private var lastMaskQuality: QualityLevel? = null
    private var lastMaskRefreshTimestampNs = Long.MIN_VALUE
    private var lastMaskFaceOnly = false
    private var lakeMix = 0f
    private val visitorReveal = VisitorRevealController()
    private val visitorSessionDetector = VisitorSessionDetector()
    @Volatile
    var revealProgressSnapshot: Float = 0f
        private set
    private var lastRenderFrameNs = 0L
    private var lastLakeTrackingTimestampNs = Long.MIN_VALUE
    private var cachedLakeFaceCenterX = 0.5f
    private var cachedLakeFaceCenterY = 0.5f
    private var cachedLakeFaceWidth = 0.38f
    private var cachedLakeFaceHeight = 0.52f
    private var hadLiveTracking = false

    /** Restarts the workshop transformation after a look is committed from the UI. */
    fun restartVisitorReveal() {
        visitorReveal.restart()
        revealProgressSnapshot = 0f
        settingsInterpolator.resetEffectsGate()
    }

    fun setMirrorTransform(
        desiredMirror: Boolean,
        surfaceContainsCameraTransform: Boolean,
        cameraTransformRequestsMirror: Boolean,
    ) {
        cameraPass.mirrorX = MirrorTransform.extraFlip(
            desiredMirror = desiredMirror,
            surfaceContainsCameraTransform = surfaceContainsCameraTransform,
            cameraTransformRequestsMirror = cameraTransformRequestsMirror,
        )
    }

    fun setCameraInputSize(width: Int, height: Int) {
        setCameraInputTransform(width, height, cameraInputRotationDegrees)
    }

    fun setCameraInputTransform(width: Int, height: Int, rotationDegrees: Int) {
        cameraInputWidth = width.coerceAtLeast(1)
        cameraInputHeight = height.coerceAtLeast(1)
        cameraInputRotationDegrees = ((rotationDegrees % 360) + 360) % 360
        cameraPass.setInputTransform(cameraInputWidth, cameraInputHeight, cameraInputRotationDegrees)
    }

    fun resize(w: Int, h: Int, maskSize: Int) {
        val sizeChanged = w != width || h != height
        val maskChanged = maskSize != maskSizeCached
        if (!sizeChanged && !maskChanged) return
        if (sizeChanged) {
            width = w
            height = h
            pingTex.allocate(w, h)
            pongTex.allocate(w, h)
            originalTex.allocate(w, h)
            pingFbo.attach(pingTex)
            pongFbo.attach(pongTex)
            originalFbo.attach(originalTex)
            listOf(
                cameraPass,
                warpPass,
                skinPass,
                underEyePass,
                lightingPass,
                detailPass,
                featurePass,
                colorPass,
                lakePass,
                compositePass,
            ).forEach { it.resize(w, h) }
            cameraPass.setInputTransform(cameraInputWidth, cameraInputHeight, cameraInputRotationDegrees)
        }
        if (maskChanged) {
            maskSizeCached = maskSize
            maskRenderer.resize(maskSize, maskSize)
        }
        if (sizeChanged || maskChanged) {
            masksValid = false
            lastMaskTrackingTimestampNs = Long.MIN_VALUE
            lastMaskRefreshTimestampNs = Long.MIN_VALUE
            lastMaskQuality = null
            lastMaskFaceOnly = false
        }
    }

    fun renderFrame(
        oesTexture: GlTexture,
        texMatrix: FloatArray,
        outputWidth: Int,
        outputHeight: Int,
        toScreen: Boolean,
    ): GlTexture {
        val t0 = SystemClock.elapsedRealtimeNanos()
        val frameDt = if (lastRenderFrameNs == 0L) {
            1f / 30f
        } else {
            ((t0 - lastRenderFrameNs) / 1_000_000_000f).coerceIn(0f, 0.10f)
        }
        lastRenderFrameNs = t0
        cameraPass.setTexMatrix(texMatrix)

        cameraPass.render(oesTexture, originalFbo, mesh)
        timing.recordPass(cameraPass.name, elapsedMs(t0))

        settingsInterpolator.setTarget(settings)
        val s = settingsInterpolator.tick(frameDt)
        val tr = tracking
        val perf = performanceState
        val comparing = s.showBeforeAfter || compareHold
        val lakeTarget = if (s.reflectionScene == ReflectionScene.DARK_LAKE && !comparing) 1f else 0f

        visitorReveal.setDuration(s.revealDurationSeconds)
        val newVisitor = visitorSessionDetector.update(tr, frameDt)
        if (newVisitor) {
            // Each visitor receives a complete transformation arc, including the arrival ripple.
            visitorReveal.restart()
            settingsInterpolator.resetEffectsGate()
        }
        val pondSceneActive = s.reflectionScene == ReflectionScene.DARK_LAKE && !comparing
        val reveal = visitorReveal.update(
            targetPresence = if (pondSceneActive) {
                if (tr.isValid) 1f else tr.effectOpacity
            } else {
                0f
            },
            deltaSeconds = frameDt,
        )
        revealProgressSnapshot = if (pondSceneActive) reveal else 1f

        if (hadLiveTracking && (!tr.isValid || tr.effectOpacity <= 0.01f)) {
            settingsInterpolator.resetEffectsGate()
            hadLiveTracking = false
        }
        // In pond mode the actual beauty graph grows with the visitor's accumulated looking time.
        // Water is already visible at t=0; only the face correction strength is gated here.
        val opacity = tr.effectOpacity * if (pondSceneActive) reveal else 1f
        val trackingLive = s.effectsEnabled && opacity > 0.01f

        // Soft-knee activation zeros weak controls so entire passes (and masks) can be skipped.
        fun amt(v: Float): Float = MathUtils.effectAmount(v * opacity)
        fun amtRaw(v: Float): Float = MathUtils.effectAmount(v)

        // Warp/feature keep opacity as a separate uniform; soft-knee the raw controls only.
        val faceSlim = amtRaw(s.faceSlimming) * perf.optionalScale
        val eyeEnlarge = amtRaw(s.eyeEnlargement) * perf.optionalScale
        val noseRefine = amtRaw(s.noseRefinement) * perf.optionalScale
        val skinStrength = amt(s.smoothingStrength)
        val complexion = amt(s.complexionEvenness)
        val redness = amt(s.rednessCorrection)
        val blemish = amt(s.blemishControl)
        val underEyeStrength = amt(s.underEyeStrength)
        val underEyeSmooth = amt(s.underEyeSmoothing)
        val underEyeColor = amt(s.underEyeColorCorrection)
        val underEyeLift = MathUtils.effectAmount((s.underEyeMaximumLift / 0.4f) * opacity) * 0.4f
        val faceExposure = amt(s.faceExposure)
        val shadowLift = amt(s.shadowLift)
        val localContrast = amt(s.localContrast)
        val shine = amt(s.shineControl)
        val skinGlow = amt(s.skinGlow)
        val eyeClarity = amt(s.eyeClarity) * (0.35f + perf.optionalScale * 0.65f)
        val eyeSparkle = amt(s.eyeSparkle) * perf.optionalScale
        val eyeBrightening = amtRaw(s.eyeBrightening)
        val browDefinition = amtRaw(s.browDefinition)
        val teethWhitening = amtRaw(s.teethWhitening)
        val lipEnhancement = amtRaw(s.lipEnhancement)
        val lipTint = amtRaw(s.lipTintStrength)
        val lipDefinition = amtRaw(s.lipDefinition)
        val lipGloss = amtRaw(s.lipGloss)
        val blush = amtRaw(s.blushStrength)
        val contour = amtRaw(s.contourStrength)
        val warmth = amt(s.warmth)

        val runWarp = s.qualityLevel.geometryEnabled &&
            opacity > 0.08f &&
            (faceSlim > 0f || eyeEnlarge > 0f || noseRefine > 0f)
        val runSkin = skinStrength > 0f || complexion > 0f || redness > 0f || blemish > 0f
        val runUnderEye = underEyeStrength > 0f ||
            underEyeSmooth > 0f ||
            underEyeColor > 0f ||
            underEyeLift > 0f
        val runLighting = kotlin.math.abs(faceExposure) > 0f ||
            shadowLift > 0f ||
            localContrast > 0f ||
            shine > 0f ||
            skinGlow > 0f
        val runDetail = s.qualityLevel.detailRestorationEnabled &&
            (eyeClarity > 0f || eyeSparkle > 0f || s.detailPreservation > 0.001f)
        val runFeature = s.qualityLevel.featureEnhancementEnabled && opacity > 0.06f && (
            eyeBrightening > 0f ||
                browDefinition > 0f ||
                teethWhitening > 0f ||
                lipEnhancement > 0f ||
                lipTint > 0f ||
                lipDefinition > 0f ||
                lipGloss > 0f ||
                blush > 0f ||
                contour > 0f
            )
        val runColor = kotlin.math.abs(warmth) > 0f
        val runAny = trackingLive && (
            runWarp || runSkin || runUnderEye || runLighting || runDetail || runFeature || runColor
            )

        val geometry = if (runAny && (runWarp || runFeature || runUnderEye)) {
            FaceEffectGeometry.from(tr.textureLandmarks, tr.headYaw, tr.headPitch)
        } else {
            null
        }

        val maskStart = SystemClock.elapsedRealtimeNanos()
        // Pond mode needs the real landmark silhouette immediately, even before the timed beauty
        // graph becomes strong enough to run. Render only that one mask during the first moments;
        // upgrade to the full mask set when beautification passes activate.
        val needPondFaceMask = lakeTarget > 0f && tr.effectOpacity > 0.01f
        val needMasks = runAny || needPondFaceMask
        val faceOnlyMask = needPondFaceMask && !runAny
        if (needMasks) {
            // Landmarks normally update at 8–20 Hz while the camera renders at ~30 FPS. Reusing
            // masks until a new tracking result arrives avoids re-drawing and blurring textures on
            // every camera frame. The idle pond never draws masks at all.
            val maskIntervalNs = perf.maskRefreshIntervalMs * 1_000_000L
            val trackingAdvanced = tr.timestampNs != lastMaskTrackingTimestampNs
            val intervalElapsed = lastMaskRefreshTimestampNs == Long.MIN_VALUE ||
                tr.timestampNs - lastMaskRefreshTimestampNs >= maskIntervalNs
            val refreshMasks = !masksValid ||
                lastMaskQuality != s.qualityLevel ||
                lastMaskFaceOnly != faceOnlyMask ||
                (trackingAdvanced && intervalElapsed)
            if (refreshMasks) {
                val polygons = maskGenerator.generate(tr)
                val faceW = tr.bounds.width.coerceAtLeast(0.15f)
                maskRenderer.render(
                    polygons = polygons,
                    // Reveal gates effect strength, not silhouette visibility. Keep the mask
                    // itself at full opacity; tracking/effect opacity is applied once by each pass.
                    opacity = 1f,
                    faceWidthNorm = faceW,
                    mesh = mesh,
                    secondBlur = s.qualityLevel.secondMaskBlur && perf.optionalScale > 0.82f,
                    faceOnly = faceOnlyMask,
                )
                masksValid = true
                lastMaskTrackingTimestampNs = tr.timestampNs
                lastMaskRefreshTimestampNs = tr.timestampNs
                lastMaskQuality = s.qualityLevel
                lastMaskFaceOnly = faceOnlyMask
            }
            if (runAny) {
                skinPass.skinMask = maskRenderer.skinMask
                skinPass.detailMask = maskRenderer.detailMask
                underEyePass.underEyeMask = maskRenderer.underEyeMask
                lightingPass.faceMask = maskRenderer.skinMask
                colorPass.faceMask = maskRenderer.skinMask
                detailPass.eyeMask = maskRenderer.eyeMask
                detailPass.detailMask = maskRenderer.detailMask
                warpPass.faceMask = maskRenderer.faceMask
                featurePass.eyeMask = maskRenderer.eyeMask
                featurePass.browMask = maskRenderer.browMask
                featurePass.lipMask = maskRenderer.lipMask
                featurePass.mouthMask = maskRenderer.mouthMask
                featurePass.faceMask = maskRenderer.faceMask
            }
        } else {
            masksValid = false
            lastMaskFaceOnly = false
        }
        timing.recordPass("masks", elapsedMs(maskStart))

        detailPass.original = originalTex
        compositePass.original = originalTex

        val faceW = tr.bounds.width.coerceAtLeast(0.15f)

        warpPass.geometry = geometry
        warpPass.opacity = opacity
        warpPass.faceSlim = faceSlim
        warpPass.eyeEnlarge = eyeEnlarge
        warpPass.noseRefine = noseRefine

        skinPass.strength = skinStrength
        skinPass.radius = s.smoothingRadius *
            (faceW / 0.35f).coerceIn(0.65f, 2.25f) *
            (height.toFloat() / 1080f).coerceIn(0.72f, 1.45f)
        skinPass.detailRetention = s.detailRetention
        skinPass.complexionEvenness = complexion
        skinPass.rednessCorrection = redness
        skinPass.blemishControl = blemish
        skinPass.sampleCount = (
            s.qualityLevel.smoothingSamples * perf.sampleScale
        ).toInt().coerceIn(2, s.qualityLevel.smoothingSamples)

        underEyePass.strength = underEyeStrength
        underEyePass.maxLift = underEyeLift
        underEyePass.colorCorrection = underEyeColor
        underEyePass.smoothing = underEyeSmooth
        underEyePass.leftVisibility = geometry?.leftVisibility ?: 1f
        underEyePass.rightVisibility = geometry?.rightVisibility ?: 1f
        underEyePass.poseWeight = geometry?.poseWeight ?: 1f
        underEyePass.leftCheek = tr.leftCheekColor
        underEyePass.rightCheek = tr.rightCheekColor
        underEyePass.leftCheekUv[0] = tr.leftCheekUv.x
        underEyePass.leftCheekUv[1] = tr.leftCheekUv.y
        underEyePass.rightCheekUv[0] = tr.rightCheekUv.x
        underEyePass.rightCheekUv[1] = tr.rightCheekUv.y

        lightingPass.faceExposure = faceExposure
        lightingPass.shadowLift = shadowLift
        lightingPass.highlightProtection = s.highlightProtection
        lightingPass.localContrast = localContrast
        lightingPass.faceLuma = tr.faceLuminance
        lightingPass.shineControl = shine
        lightingPass.skinGlow = skinGlow

        detailPass.eyeClarity = eyeClarity
        detailPass.eyeSparkle = eyeSparkle
        detailPass.detailPreservation = s.detailPreservation

        featurePass.geometry = geometry
        featurePass.opacity = opacity
        featurePass.eyeBrightening = eyeBrightening
        featurePass.browDefinition = browDefinition
        featurePass.teethWhitening = teethWhitening
        featurePass.lipEnhancement = lipEnhancement
        featurePass.lipTintStrength = lipTint
        featurePass.lipDefinition = lipDefinition
        featurePass.lipGloss = lipGloss
        featurePass.blush = blush
        featurePass.contour = contour

        colorPass.warmth = warmth
        colorPass.localContrast = 0f
        compositePass.beforeAfter = if (comparing) 1f else 0f
        compositePass.dimAmount = settingsInterpolator.dimAmount()

        val lakeAlphaAt30 = if (lakeTarget > lakeMix) 0.10f else 0.14f
        val lakeAlpha = 1f - (1f - lakeAlphaAt30).pow(frameDt * 30f)
        lakeMix += (lakeTarget - lakeMix) * lakeAlpha
        if (kotlin.math.abs(lakeTarget - lakeMix) < 0.002f) lakeMix = lakeTarget

        // Face bounds only change when analysis advances. Do not scan 478 points at camera FPS.
        if (lakeTarget > 0f && tr.timestampNs != lastLakeTrackingTimestampNs) {
            val texturePoints = tr.textureLandmarks
            if (texturePoints.isNotEmpty()) {
                var minX = 1f
                var minY = 1f
                var maxX = 0f
                var maxY = 0f
                texturePoints.forEach { point ->
                    minX = minOf(minX, point.x)
                    minY = minOf(minY, point.y)
                    maxX = maxOf(maxX, point.x)
                    maxY = maxOf(maxY, point.y)
                }
                cachedLakeFaceCenterX = (minX + maxX) * 0.5f
                cachedLakeFaceCenterY = (minY + maxY) * 0.5f
                // Slight pad: the ellipse is only a cheap shader gate around the real face mask.
                cachedLakeFaceWidth = ((maxX - minX) * 1.06f).coerceIn(0.12f, 0.85f)
                cachedLakeFaceHeight = ((maxY - minY) * 1.08f).coerceIn(0.16f, 0.95f)
            }
            lastLakeTrackingTimestampNs = tr.timestampNs
        }

        lakePass.faceCenterX = cachedLakeFaceCenterX
        lakePass.faceCenterY = cachedLakeFaceCenterY
        lakePass.faceWidth = cachedLakeFaceWidth
        lakePass.faceHeight = cachedLakeFaceHeight
        lakePass.faceMask = if (needPondFaceMask && masksValid) maskRenderer.faceMask else null
        lakePass.facePresence = tr.effectOpacity
        lakePass.visitorReveal = reveal
        lakePass.timeSeconds = (SystemClock.elapsedRealtime() % 3_600_000L) / 1_000f
        lakePass.intensity = s.lakeIntensity * lakeMix
        lakePass.motion = s.lakeMotion
        lakePass.darkness = s.lakeDarkness
        // 1.0 keeps the isolated face fully readable; shader water veil remains deliberately tiny.
        lakePass.faceClarity = s.lakeFaceClarity
        lakePass.quality = if (s.qualityLevel == QualityLevel.PERFORMANCE) {
            0.18f
        } else {
            perf.sampleScale
        }
        lakePass.enabled = lakeMix > 0.003f

        var read: GlTexture = originalTex
        var writeFbo = pingFbo
        var writeTex = pingTex

        fun run(pass: RenderPass) {
            val start = SystemClock.elapsedRealtimeNanos()
            pass.render(read, writeFbo, mesh)
            timing.recordPass(pass.name, elapsedMs(start))
            read = writeTex
            if (writeFbo === pingFbo) {
                writeFbo = pongFbo
                writeTex = pongTex
            } else {
                writeFbo = pingFbo
                writeTex = pingTex
            }
        }

        var effectsRan = false
        if (runAny) {
            if (runWarp && geometry != null) {
                run(warpPass)
                effectsRan = true
            }
            if (runSkin) {
                run(skinPass)
                effectsRan = true
            }
            if (runUnderEye) {
                run(underEyePass)
                effectsRan = true
            }
            if (runLighting) {
                run(lightingPass)
                effectsRan = true
            }
            if (runDetail && (
                    eyeClarity > 0f ||
                        eyeSparkle > 0f ||
                        (read !== originalTex && s.detailPreservation > 0.001f)
                    )
            ) {
                run(detailPass)
                effectsRan = true
            }
            if (runFeature && geometry != null) {
                run(featurePass)
                effectsRan = true
            }
            if (runColor) {
                run(colorPass)
                effectsRan = true
            }
        }

        if (lakePass.enabled) {
            run(lakePass)
        }

        if (effectsRan) {
            hadLiveTracking = true
            settingsInterpolator.markEffectsApplied()
        } else if (!s.effectsEnabled) {
            // Nothing to wait for when beautify is off — allow lerp for the next enable.
            settingsInterpolator.markEffectsApplied()
        }

        if (toScreen) {
            compositePass.renderToScreen(read, mesh, outputWidth, outputHeight)
            lastOutputFbo = null
        } else {
            compositePass.render(read, writeFbo, mesh)
            read = writeTex
            lastOutputFbo = writeFbo
        }

        timing.recordGpuFrame(elapsedMs(t0))
        timing.markCameraFrame()
        return read
    }

    fun bindLastOutputForRead(): Boolean {
        val fbo = lastOutputFbo ?: return false
        fbo.bind()
        return true
    }

    fun unbindLastOutput() {
        lastOutputFbo?.unbind()
    }

    fun release() {
        cameraPass.release()
        warpPass.release()
        skinPass.release()
        underEyePass.release()
        lightingPass.release()
        detailPass.release()
        featurePass.release()
        colorPass.release()
        lakePass.release()
        compositePass.release()
        maskRenderer.release()
        mesh.delete()
        pingTex.delete(); pongTex.delete(); originalTex.delete()
        pingFbo.delete(); pongFbo.delete(); originalFbo.delete()
    }

    private fun elapsedMs(startNs: Long) =
        (SystemClock.elapsedRealtimeNanos() - startNs) / 1e6
}
