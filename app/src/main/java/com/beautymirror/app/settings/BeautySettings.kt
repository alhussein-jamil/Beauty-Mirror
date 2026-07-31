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
            globalStrength = 0.72f,
            smoothingStrength = 0.66f,
            smoothingRadius = 6.4f,
            detailRetention = 0.56f,
            complexionEvenness = 0.48f,
            rednessCorrection = 0.30f,
            blemishControl = 0.42f,
            shineControl = 0.38f,
            skinGlow = 0.26f,
            underEyeStrength = 0.58f,
            underEyeSmoothing = 0.44f,
            underEyeMaximumLift = 0.18f,
            underEyeColorCorrection = 0.34f,
            faceExposure = 0.055f,
            shadowLift = 0.11f,
            highlightProtection = 0.62f,
            warmth = 0.07f,
            localContrast = 0.10f,
            contourStrength = 0.28f,
            blushStrength = 0.18f,
            eyeClarity = 0.34f,
            eyeBrightening = 0.18f,
            eyeSparkle = 0.30f,
            browDefinition = 0.24f,
            teethWhitening = 0.24f,
            lipEnhancement = 0.36f,
            lipTintStrength = 0.38f,
            lipDefinition = 0.36f,
            lipGloss = 0.24f,
            detailPreservation = 0.84f,
            faceSlimming = 0.22f,
            eyeEnlargement = 0.15f,
            noseRefinement = 0.12f,
            qualityLevel = QualityLevel.HIGH,
        )

        fun glam(): BeautySettings = BeautySettings(
            preset = BeautyPreset.GLAM,
            globalStrength = 0.82f,
            smoothingStrength = 0.78f,
            smoothingRadius = 7.0f,
            detailRetention = 0.48f,
            complexionEvenness = 0.58f,
            rednessCorrection = 0.38f,
            blemishControl = 0.56f,
            shineControl = 0.46f,
            skinGlow = 0.34f,
            underEyeStrength = 0.68f,
            underEyeSmoothing = 0.58f,
            underEyeMaximumLift = 0.20f,
            underEyeColorCorrection = 0.40f,
            faceExposure = 0.045f,
            shadowLift = 0.12f,
            highlightProtection = 0.60f,
            warmth = 0.10f,
            localContrast = 0.12f,
            contourStrength = 0.38f,
            blushStrength = 0.28f,
            eyeClarity = 0.44f,
            eyeBrightening = 0.26f,
            eyeSparkle = 0.42f,
            browDefinition = 0.38f,
            teethWhitening = 0.30f,
            lipEnhancement = 0.52f,
            lipTintStrength = 0.54f,
            lipDefinition = 0.50f,
            lipGloss = 0.34f,
            detailPreservation = 0.80f,
            faceSlimming = 0.32f,
            eyeEnlargement = 0.22f,
            noseRefinement = 0.18f,
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
            )
        }

        fun fromGlobalStrength(strength: Float, base: BeautySettings = natural()): BeautySettings {
            val g = MathUtils.clamp(strength, 0f, 1f)
            return base.copy(
                preset = if (g <= 0.001f) BeautyPreset.OFF else BeautyPreset.CUSTOM,
                globalStrength = g,
                smoothingRadius = 4.0f + g * 3.0f,
                detailRetention = 0.78f - g * 0.30f,
                smoothingStrength = g * 0.88f,
                complexionEvenness = g * 0.64f,
                rednessCorrection = g * 0.42f,
                blemishControl = g * 0.58f,
                shineControl = g * 0.46f,
                skinGlow = g * 0.34f,
                underEyeStrength = g * 0.72f,
                underEyeSmoothing = g * 0.60f,
                underEyeMaximumLift = g * 0.18f,
                underEyeColorCorrection = g * 0.48f,
                faceExposure = g * 0.08f,
                shadowLift = g * 0.15f,
                eyeClarity = g * 0.50f,
                eyeBrightening = g * 0.28f,
                eyeSparkle = g * 0.42f,
                browDefinition = g * 0.36f,
                teethWhitening = g * 0.24f,
                lipEnhancement = g * 0.58f,
                lipTintStrength = g * 0.56f,
                lipDefinition = g * 0.50f,
                lipGloss = g * 0.34f,
                contourStrength = g * 0.38f,
                blushStrength = g * 0.24f,
                faceSlimming = g * 0.34f,
                eyeEnlargement = g * 0.23f,
                noseRefinement = g * 0.18f,
                warmth = g * 0.10f,
                localContrast = g * 0.14f,
                qualityLevel = base.qualityLevel,
                debugOverlay = base.debugOverlay,
                mirrorPreview = base.mirrorPreview,
            ).clamped()
        }
    }
}
