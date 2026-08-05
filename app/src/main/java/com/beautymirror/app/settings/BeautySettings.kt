package com.beautymirror.app.settings

import com.beautymirror.app.util.MathUtils

/** Immutable real-time beautification parameters. */
data class BeautySettings(
    val preset: BeautyPreset = BeautyPreset.NATURAL,
    val globalStrength: Float = 0.44f,

    // Skin and complexion.
    val smoothingStrength: Float = 0.32f,
    val smoothingRadius: Float = 4.2f,
    val detailRetention: Float = 0.72f,
    val complexionEvenness: Float = 0.16f,
    val rednessCorrection: Float = 0.06f,
    val blemishControl: Float = 0.10f,
    val shineControl: Float = 0.10f,
    val skinGlow: Float = 0.08f,
    val underEyeStrength: Float = 0.24f,
    val underEyeSmoothing: Float = 0.12f,
    val underEyeMaximumLift: Float = 0.12f,
    val underEyeColorCorrection: Float = 0.24f,
    val faceExposure: Float = 0.04f,
    val shadowLift: Float = 0.06f,
    val highlightProtection: Float = 0.40f,
    val warmth: Float = 0.05f,
    val localContrast: Float = 0.07f,
    val contourStrength: Float = 0.08f,
    val blushStrength: Float = 0.06f,

    // Feature enhancement.
    val eyeClarity: Float = 0.16f,
    val eyeBrightening: Float = 0.08f,
    val eyeSparkle: Float = 0.08f,
    val browDefinition: Float = 0.08f,
    val teethWhitening: Float = 0.07f,
    val lipEnhancement: Float = 0.14f,
    val lipTintStrength: Float = 0.10f,
    val lipDefinition: Float = 0.12f,
    val lipGloss: Float = 0.07f,
    val detailPreservation: Float = 0.78f,

    // Pose-aware geometry. Restrained defaults keep identity stable.
    val faceSlimming: Float = 0.08f,
    val eyeEnlargement: Float = 0.06f,
    val noseRefinement: Float = 0.05f,

    // Exhibition scene. The water pass runs after beautification so corrections remain visible.
    val reflectionScene: ReflectionScene = ReflectionScene.DARK_LAKE,
    val lakeIntensity: Float = 0.92f,
    val lakeMotion: Float = 0.55f,
    val lakeDarkness: Float = 0.08f,
    val lakeFaceClarity: Float = 0.90f,
    val lakeCameraBlend: Float = 0.58f,
    val lakeDeformation: Float = 0.40f,
    val lakeSwirl: Float = 0.70f,
    /** Water amount after reveal settles (0 = none, 1 = strong water veil). */
    val lakeSettledWater: Float = 0.42f,
    /** Camera amount after reveal settles (0 = none, 1 = strong camera). */
    val lakeSettledCamera: Float = 0.72f,
    /** How many independent ripple regions (0 = none, 1 = six). */
    val lakeRippleRegions: Float = 0.40f,
    val lakeRippleSpeed: Float = 0.55f,
    /** Directional wave / chop complexity. */
    val lakeWaveDetail: Float = 0.55f,
    val lakeSpecular: Float = 0.50f,
    /** Lift toward light sky-blue (vs deeper teal). */
    val lakeSkyBlue: Float = 0.78f,
    /** How much sunlight hits the pond surface. */
    val lakeSunlight: Float = 0.70f,
    /** Cool cyan (0) → warm amber (1) water tint. */
    val lakeWaterWarmth: Float = 0.22f,
    /** Color saturation of sky and water. */
    val lakeSaturation: Float = 0.78f,
    /** Crest / foam brightness on wave peaks. */
    val lakeFoam: Float = 0.55f,
    /** Soft cloud amount in the reflected sky. */
    val lakeClouds: Float = 0.55f,
    val revealDurationSeconds: Float = 10f,

    val qualityLevel: QualityLevel = QualityLevel.MEDIUM,
    val showBeforeAfter: Boolean = false,
    val debugOverlay: Boolean = false,
    val mirrorPreview: Boolean = true,
) {
    val effectsEnabled: Boolean
        get() = preset != BeautyPreset.OFF && !showBeforeAfter && (
            smoothingStrength > 0.001f ||
                complexionEvenness > 0.001f ||
                rednessCorrection > 0.001f ||
                blemishControl > 0.001f ||
                shineControl > 0.001f ||
                skinGlow > 0.001f ||
                underEyeStrength > 0.001f ||
                underEyeSmoothing > 0.001f ||
                underEyeMaximumLift > 0.001f ||
                underEyeColorCorrection > 0.001f ||
                kotlin.math.abs(faceExposure) > 0.001f ||
                shadowLift > 0.001f ||
                kotlin.math.abs(warmth) > 0.001f ||
                localContrast > 0.001f ||
                contourStrength > 0.001f ||
                blushStrength > 0.001f ||
                eyeClarity > 0.001f ||
                eyeBrightening > 0.001f ||
                eyeSparkle > 0.001f ||
                browDefinition > 0.001f ||
                teethWhitening > 0.001f ||
                lipEnhancement > 0.001f ||
                lipTintStrength > 0.001f ||
                lipDefinition > 0.001f ||
                lipGloss > 0.001f ||
                faceSlimming > 0.001f ||
                eyeEnlargement > 0.001f ||
                noseRefinement > 0.001f
            )

    fun clamped(): BeautySettings = copy(
        globalStrength = MathUtils.clamp(globalStrength, 0f, 1f),
        smoothingStrength = MathUtils.clamp(smoothingStrength, 0f, 1f),
        smoothingRadius = MathUtils.clamp(smoothingRadius, 0.5f, 8f),
        detailRetention = MathUtils.clamp(detailRetention, 0f, 1f),
        complexionEvenness = MathUtils.clamp(complexionEvenness, 0f, 1f),
        rednessCorrection = MathUtils.clamp(rednessCorrection, 0f, 1f),
        blemishControl = MathUtils.clamp(blemishControl, 0f, 1f),
        shineControl = MathUtils.clamp(shineControl, 0f, 1f),
        skinGlow = MathUtils.clamp(skinGlow, 0f, 1f),
        underEyeStrength = MathUtils.clamp(underEyeStrength, 0f, 1f),
        underEyeSmoothing = MathUtils.clamp(underEyeSmoothing, 0f, 1f),
        underEyeMaximumLift = MathUtils.clamp(underEyeMaximumLift, 0f, 0.4f),
        underEyeColorCorrection = MathUtils.clamp(underEyeColorCorrection, 0f, 1f),
        faceExposure = MathUtils.clamp(faceExposure, -0.3f, 0.5f),
        shadowLift = MathUtils.clamp(shadowLift, 0f, 0.5f),
        highlightProtection = MathUtils.clamp(highlightProtection, 0f, 1f),
        warmth = MathUtils.clamp(warmth, -0.5f, 0.5f),
        localContrast = MathUtils.clamp(localContrast, 0f, 0.6f),
        contourStrength = MathUtils.clamp(contourStrength, 0f, 1f),
        blushStrength = MathUtils.clamp(blushStrength, 0f, 1f),
        eyeClarity = MathUtils.clamp(eyeClarity, 0f, 0.8f),
        eyeBrightening = MathUtils.clamp(eyeBrightening, 0f, 1f),
        eyeSparkle = MathUtils.clamp(eyeSparkle, 0f, 1f),
        browDefinition = MathUtils.clamp(browDefinition, 0f, 1f),
        teethWhitening = MathUtils.clamp(teethWhitening, 0f, 1f),
        lipEnhancement = MathUtils.clamp(lipEnhancement, 0f, 1f),
        lipTintStrength = MathUtils.clamp(lipTintStrength, 0f, 1f),
        lipDefinition = MathUtils.clamp(lipDefinition, 0f, 1f),
        lipGloss = MathUtils.clamp(lipGloss, 0f, 1f),
        detailPreservation = MathUtils.clamp(detailPreservation, 0f, 1f),
        faceSlimming = MathUtils.clamp(faceSlimming, 0f, 1f),
        eyeEnlargement = MathUtils.clamp(eyeEnlargement, 0f, 1f),
        noseRefinement = MathUtils.clamp(noseRefinement, 0f, 1f),
        lakeIntensity = MathUtils.clamp(lakeIntensity, 0f, 1f),
        lakeMotion = MathUtils.clamp(lakeMotion, 0f, 1f),
        lakeDarkness = MathUtils.clamp(lakeDarkness, 0f, 1f),
        lakeFaceClarity = MathUtils.clamp(lakeFaceClarity, 0f, 1f),
        lakeCameraBlend = MathUtils.clamp(lakeCameraBlend, 0f, 1f),
        lakeDeformation = MathUtils.clamp(lakeDeformation, 0f, 1f),
        lakeSwirl = MathUtils.clamp(lakeSwirl, 0f, 1f),
        lakeSettledWater = MathUtils.clamp(lakeSettledWater, 0f, 1f),
        lakeSettledCamera = MathUtils.clamp(lakeSettledCamera, 0f, 1f),
        lakeRippleRegions = MathUtils.clamp(lakeRippleRegions, 0f, 1f),
        lakeRippleSpeed = MathUtils.clamp(lakeRippleSpeed, 0f, 1f),
        lakeWaveDetail = MathUtils.clamp(lakeWaveDetail, 0f, 1f),
        lakeSpecular = MathUtils.clamp(lakeSpecular, 0f, 1f),
        lakeSkyBlue = MathUtils.clamp(lakeSkyBlue, 0f, 1f),
        lakeSunlight = MathUtils.clamp(lakeSunlight, 0f, 1f),
        lakeWaterWarmth = MathUtils.clamp(lakeWaterWarmth, 0f, 1f),
        lakeSaturation = MathUtils.clamp(lakeSaturation, 0f, 1f),
        lakeFoam = MathUtils.clamp(lakeFoam, 0f, 1f),
        lakeClouds = MathUtils.clamp(lakeClouds, 0f, 1f),
        revealDurationSeconds = MathUtils.clamp(
            revealDurationSeconds,
            VisitorRevealController.MIN_DURATION_SECONDS,
            VisitorRevealController.MAX_DURATION_SECONDS,
        ),
    )

    companion object {
        fun off(): BeautySettings = BeautySettings(
            preset = BeautyPreset.OFF,
            globalStrength = 0f,
            smoothingStrength = 0f,
            complexionEvenness = 0f,
            rednessCorrection = 0f,
            blemishControl = 0f,
            shineControl = 0f,
            skinGlow = 0f,
            underEyeStrength = 0f,
            underEyeSmoothing = 0f,
            underEyeMaximumLift = 0f,
            underEyeColorCorrection = 0f,
            faceExposure = 0f,
            shadowLift = 0f,
            warmth = 0f,
            localContrast = 0f,
            contourStrength = 0f,
            blushStrength = 0f,
            eyeClarity = 0f,
            eyeBrightening = 0f,
            eyeSparkle = 0f,
            browDefinition = 0f,
            teethWhitening = 0f,
            lipEnhancement = 0f,
            lipTintStrength = 0f,
            lipDefinition = 0f,
            lipGloss = 0f,
            faceSlimming = 0f,
            eyeEnlargement = 0f,
            noseRefinement = 0f,
        )

        fun natural(): BeautySettings = BeautySettings()

        fun soft(): BeautySettings = BeautySettings(
            preset = BeautyPreset.SOFT,
            globalStrength = 0.58f,
            smoothingStrength = 0.53f,
            smoothingRadius = 5.2f,
            detailRetention = 0.62f,
            complexionEvenness = 0.31f,
            rednessCorrection = 0.10f,
            blemishControl = 0.24f,
            shineControl = 0.16f,
            skinGlow = 0.10f,
            underEyeStrength = 0.40f,
            underEyeSmoothing = 0.28f,
            underEyeMaximumLift = 0.16f,
            faceExposure = 0.035f,
            shadowLift = 0.08f,
            warmth = 0.07f,
            localContrast = 0.03f,
            contourStrength = 0.02f,
            blushStrength = 0.04f,
            eyeClarity = 0.17f,
            eyeBrightening = 0.07f,
            eyeSparkle = 0.10f,
            browDefinition = 0.08f,
            teethWhitening = 0.06f,
            lipEnhancement = 0.18f,
            lipTintStrength = 0.14f,
            lipDefinition = 0.14f,
            lipGloss = 0.08f,
            faceSlimming = 0.10f,
            eyeEnlargement = 0.07f,
            noseRefinement = 0.055f,
        )

        fun bright(): BeautySettings = BeautySettings(
            preset = BeautyPreset.BRIGHT,
            globalStrength = 0.56f,
            smoothingStrength = 0.42f,
            smoothingRadius = 4.8f,
            complexionEvenness = 0.28f,
            rednessCorrection = 0.10f,
            blemishControl = 0.18f,
            shineControl = 0.24f,
            skinGlow = 0.20f,
            underEyeStrength = 0.30f,
            underEyeSmoothing = 0.20f,
            faceExposure = 0.18f,
            shadowLift = 0.16f,
            warmth = 0.08f,
            localContrast = 0.10f,
            contourStrength = 0.05f,
            blushStrength = 0.03f,
            eyeClarity = 0.13f,
            eyeBrightening = 0.15f,
            eyeSparkle = 0.18f,
            browDefinition = 0.10f,
            teethWhitening = 0.13f,
            lipEnhancement = 0.12f,
            lipTintStrength = 0.12f,
            lipDefinition = 0.10f,
            lipGloss = 0.07f,
            highlightProtection = 0.52f,
            detailRetention = 0.72f,
            faceSlimming = 0.09f,
            eyeEnlargement = 0.065f,
            noseRefinement = 0.05f,
        )

        /** Tuned for bright exhibition lighting and spectators standing at different angles. */
        fun stage(): BeautySettings = BeautySettings(
            preset = BeautyPreset.STAGE,
            globalStrength = 0.82f,
            smoothingStrength = 0.70f,
            smoothingRadius = 6.6f,
            detailRetention = 0.54f,
            complexionEvenness = 0.54f,
            rednessCorrection = 0.34f,
            blemishControl = 0.48f,
            shineControl = 0.42f,
            skinGlow = 0.28f,
            underEyeStrength = 0.64f,
            underEyeSmoothing = 0.50f,
            underEyeMaximumLift = 0.22f,
            underEyeColorCorrection = 0.46f,
            faceExposure = 0.06f,
            shadowLift = 0.12f,
            highlightProtection = 0.64f,
            warmth = 0.07f,
            localContrast = 0.11f,
            contourStrength = 0.34f,
            blushStrength = 0.20f,
            eyeClarity = 0.40f,
            eyeBrightening = 0.24f,
            eyeSparkle = 0.36f,
            browDefinition = 0.30f,
            teethWhitening = 0.28f,
            lipEnhancement = 0.44f,
            lipTintStrength = 0.46f,
            lipDefinition = 0.44f,
            lipGloss = 0.28f,
            detailPreservation = 0.84f,
            faceSlimming = 0.28f,
            eyeEnlargement = 0.18f,
            noseRefinement = 0.15f,
            qualityLevel = QualityLevel.HIGH,
        )

        fun glam(): BeautySettings = BeautySettings(
            preset = BeautyPreset.GLAM,
            globalStrength = 0.92f,
            smoothingStrength = 0.82f,
            smoothingRadius = 7.2f,
            detailRetention = 0.44f,
            complexionEvenness = 0.66f,
            rednessCorrection = 0.44f,
            blemishControl = 0.66f,
            shineControl = 0.52f,
            skinGlow = 0.38f,
            underEyeStrength = 0.78f,
            underEyeSmoothing = 0.64f,
            underEyeMaximumLift = 0.24f,
            underEyeColorCorrection = 0.54f,
            faceExposure = 0.05f,
            shadowLift = 0.13f,
            highlightProtection = 0.62f,
            warmth = 0.10f,
            localContrast = 0.14f,
            contourStrength = 0.48f,
            blushStrength = 0.32f,
            eyeClarity = 0.52f,
            eyeBrightening = 0.32f,
            eyeSparkle = 0.48f,
            browDefinition = 0.46f,
            teethWhitening = 0.34f,
            lipEnhancement = 0.62f,
            lipTintStrength = 0.62f,
            lipDefinition = 0.60f,
            lipGloss = 0.42f,
            detailPreservation = 0.80f,
            faceSlimming = 0.42f,
            eyeEnlargement = 0.28f,
            noseRefinement = 0.22f,
            qualityLevel = QualityLevel.HIGH,
        )


        fun fromPreset(preset: BeautyPreset, current: BeautySettings = natural()): BeautySettings {
            val base = when (preset) {
                BeautyPreset.OFF -> off()
                BeautyPreset.NATURAL -> natural()
                BeautyPreset.SOFT -> soft()
                BeautyPreset.BRIGHT -> bright()
                BeautyPreset.STAGE -> stage()
                BeautyPreset.GLAM -> glam()
                BeautyPreset.CUSTOM -> current.copy(preset = BeautyPreset.CUSTOM)
            }
            return base.copy(
                qualityLevel = if (preset == BeautyPreset.STAGE || preset == BeautyPreset.GLAM) {
                    base.qualityLevel
                } else {
                    current.qualityLevel
                },
                debugOverlay = current.debugOverlay,
                mirrorPreview = current.mirrorPreview,
                reflectionScene = current.reflectionScene,
                lakeIntensity = current.lakeIntensity,
                lakeMotion = current.lakeMotion,
                lakeDarkness = current.lakeDarkness,
                lakeFaceClarity = current.lakeFaceClarity,
                lakeCameraBlend = current.lakeCameraBlend,
                lakeDeformation = current.lakeDeformation,
                lakeSwirl = current.lakeSwirl,
                lakeSettledWater = current.lakeSettledWater,
                lakeSettledCamera = current.lakeSettledCamera,
                lakeRippleRegions = current.lakeRippleRegions,
                lakeRippleSpeed = current.lakeRippleSpeed,
                lakeWaveDetail = current.lakeWaveDetail,
                lakeSpecular = current.lakeSpecular,
                lakeSkyBlue = current.lakeSkyBlue,
                lakeSunlight = current.lakeSunlight,
                lakeWaterWarmth = current.lakeWaterWarmth,
                lakeSaturation = current.lakeSaturation,
                lakeFoam = current.lakeFoam,
                lakeClouds = current.lakeClouds,
                revealDurationSeconds = current.revealDurationSeconds,
            )
        }

        fun fromGlobalStrength(strength: Float, base: BeautySettings = natural()): BeautySettings {
            val g = MathUtils.clamp(strength, 0f, 1f)
            val gSoft = kotlin.math.sqrt(g)
            return base.copy(
                preset = if (g <= 0.001f) BeautyPreset.OFF else BeautyPreset.CUSTOM,
                globalStrength = g,
                smoothingRadius = 4.0f + g * 3.3f,
                detailRetention = 0.80f - g * 0.34f,
                smoothingStrength = (g * 0.94f).coerceAtMost(1f),
                complexionEvenness = (g * 0.72f).coerceAtMost(1f),
                rednessCorrection = (g * 0.50f).coerceAtMost(1f),
                blemishControl = (g * 0.68f).coerceAtMost(1f),
                shineControl = (g * 0.54f).coerceAtMost(1f),
                skinGlow = (g * 0.38f).coerceAtMost(1f),
                underEyeStrength = (g * 0.84f).coerceAtMost(1f),
                underEyeSmoothing = (g * 0.68f).coerceAtMost(1f),
                underEyeMaximumLift = g * 0.22f,
                underEyeColorCorrection = (g * 0.60f).coerceAtMost(1f),
                faceExposure = g * 0.09f,
                shadowLift = g * 0.18f,
                eyeClarity = g * 0.56f,
                eyeBrightening = g * 0.34f,
                eyeSparkle = g * 0.48f,
                browDefinition = g * 0.44f,
                teethWhitening = g * 0.28f,
                lipEnhancement = g * 0.68f,
                lipTintStrength = g * 0.64f,
                lipDefinition = g * 0.62f,
                lipGloss = g * 0.42f,
                contourStrength = g * 0.48f,
                blushStrength = g * 0.30f,
                faceSlimming = gSoft * 0.42f,
                eyeEnlargement = gSoft * 0.28f,
                noseRefinement = gSoft * 0.22f,
                warmth = g * 0.10f,
                localContrast = g * 0.16f,
                qualityLevel = base.qualityLevel,
                debugOverlay = base.debugOverlay,
                mirrorPreview = base.mirrorPreview,
                reflectionScene = base.reflectionScene,
                lakeIntensity = base.lakeIntensity,
                lakeMotion = base.lakeMotion,
                lakeDarkness = base.lakeDarkness,
                lakeFaceClarity = base.lakeFaceClarity,
                lakeCameraBlend = base.lakeCameraBlend,
                lakeDeformation = base.lakeDeformation,
                lakeSwirl = base.lakeSwirl,
                revealDurationSeconds = base.revealDurationSeconds,
            ).clamped()
        }
    }
}
