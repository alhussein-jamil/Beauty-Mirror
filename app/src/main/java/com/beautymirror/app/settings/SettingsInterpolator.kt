package com.beautymirror.app.settings

import com.beautymirror.app.util.MathUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

/**
 * Smooths beauty parameters for the GPU path.
 *
 * Interpolation stays off until [markEffectsApplied] runs at least once (full effect stack
 * with live tracking). Until then target snaps through. Interpolation is normalized to a 30 FPS
 * reference so the same slider movement feels identical on 24, 30, 60 or variable-rate previews.
 */
class SettingsInterpolator(
    private val lerpAlpha: Float = 0.28f,
    private val dimPulse: Float = 0.22f,
    private val dimDecay: Float = 0.70f,
) {
    private var target: BeautySettings = BeautySettings.off()
    private var current: BeautySettings = BeautySettings.off()
    private var effectsAppliedOnce: Boolean = false
    private var dimAmount: Float = 0f
    private var pendingVisualChange: Boolean = false

    fun displayed(): BeautySettings = current

    fun dimAmount(): Float = dimAmount

    fun effectsReady(): Boolean = effectsAppliedOnce

    fun setTarget(next: BeautySettings) {
        val clamped = next.clamped()
        if (visualParamsDiffer(target, clamped)) {
            pendingVisualChange = true
            dimAmount = max(dimAmount, dimPulse)
        }
        target = clamped
        if (!effectsAppliedOnce) {
            // Cold path: snap until the pipeline has applied effects once.
            current = clamped
        }
    }

    /**
     * Call after a render frame where masks + enabled beauty passes actually ran
     * (or effects are off / no face so there is nothing to wait for).
     */
    fun markEffectsApplied() {
        if (!effectsAppliedOnce) {
            effectsAppliedOnce = true
            current = target
        }
    }

    fun resetEffectsGate() {
        effectsAppliedOnce = false
        pendingVisualChange = false
    }

    /** Advance one display frame. Returns settings the GPU should use. */
    fun tick(deltaSeconds: Float = 1f / 30f): BeautySettings {
        val referenceFrames = (deltaSeconds.coerceIn(0f, 0.10f) * 30f).coerceAtLeast(0f)
        val frameAlpha = 1f - (1f - lerpAlpha.coerceIn(0f, 1f)).pow(referenceFrames)
        val frameDimDecay = dimDecay.coerceIn(0f, 1f).pow(referenceFrames)
        if (!effectsAppliedOnce) {
            current = target
            dimAmount *= frameDimDecay
            if (dimAmount < 0.01f) dimAmount = 0f
            return current
        }

        // After the first full apply, blend toward the latest target.
        current = lerpSettings(current, target, frameAlpha)
        current = mergeNonLerped(target, current)

        if (pendingVisualChange && nearTarget(current, target)) {
            pendingVisualChange = false
            current = mergeNonLerped(target, target)
        }

        dimAmount *= frameDimDecay
        if (dimAmount < 0.01f) dimAmount = 0f
        return current
    }

    companion object {
        fun visualParamsDiffer(a: BeautySettings, b: BeautySettings): Boolean {
            return abs(a.globalStrength - b.globalStrength) > 0.001f ||
                abs(a.smoothingStrength - b.smoothingStrength) > 0.001f ||
                abs(a.smoothingRadius - b.smoothingRadius) > 0.001f ||
                abs(a.detailRetention - b.detailRetention) > 0.001f ||
                abs(a.complexionEvenness - b.complexionEvenness) > 0.001f ||
                abs(a.rednessCorrection - b.rednessCorrection) > 0.001f ||
                abs(a.blemishControl - b.blemishControl) > 0.001f ||
                abs(a.shineControl - b.shineControl) > 0.001f ||
                abs(a.skinGlow - b.skinGlow) > 0.001f ||
                abs(a.underEyeStrength - b.underEyeStrength) > 0.001f ||
                abs(a.underEyeSmoothing - b.underEyeSmoothing) > 0.001f ||
                abs(a.underEyeMaximumLift - b.underEyeMaximumLift) > 0.001f ||
                abs(a.underEyeColorCorrection - b.underEyeColorCorrection) > 0.001f ||
                abs(a.faceExposure - b.faceExposure) > 0.001f ||
                abs(a.shadowLift - b.shadowLift) > 0.001f ||
                abs(a.highlightProtection - b.highlightProtection) > 0.001f ||
                abs(a.warmth - b.warmth) > 0.001f ||
                abs(a.localContrast - b.localContrast) > 0.001f ||
                abs(a.contourStrength - b.contourStrength) > 0.001f ||
                abs(a.blushStrength - b.blushStrength) > 0.001f ||
                abs(a.eyeClarity - b.eyeClarity) > 0.001f ||
                abs(a.eyeBrightening - b.eyeBrightening) > 0.001f ||
                abs(a.eyeSparkle - b.eyeSparkle) > 0.001f ||
                abs(a.browDefinition - b.browDefinition) > 0.001f ||
                abs(a.teethWhitening - b.teethWhitening) > 0.001f ||
                abs(a.lipEnhancement - b.lipEnhancement) > 0.001f ||
                abs(a.lipTintStrength - b.lipTintStrength) > 0.001f ||
                abs(a.lipDefinition - b.lipDefinition) > 0.001f ||
                abs(a.lipGloss - b.lipGloss) > 0.001f ||
                abs(a.detailPreservation - b.detailPreservation) > 0.001f ||
                abs(a.faceSlimming - b.faceSlimming) > 0.001f ||
                abs(a.eyeEnlargement - b.eyeEnlargement) > 0.001f ||
                abs(a.noseRefinement - b.noseRefinement) > 0.001f ||
                abs(a.lakeIntensity - b.lakeIntensity) > 0.001f ||
                abs(a.lakeMotion - b.lakeMotion) > 0.001f ||
                abs(a.lakeDarkness - b.lakeDarkness) > 0.001f ||
                abs(a.lakeFaceClarity - b.lakeFaceClarity) > 0.001f ||
                a.reflectionScene != b.reflectionScene ||
                a.preset != b.preset ||
                a.effectsEnabled != b.effectsEnabled
        }

        private fun nearTarget(a: BeautySettings, b: BeautySettings): Boolean =
            !visualParamsDiffer(a, b)

        private fun mergeNonLerped(from: BeautySettings, into: BeautySettings): BeautySettings =
            into.copy(
                preset = from.preset,
                qualityLevel = from.qualityLevel,
                showBeforeAfter = from.showBeforeAfter,
                debugOverlay = from.debugOverlay,
                mirrorPreview = from.mirrorPreview,
                reflectionScene = from.reflectionScene,
                revealDurationSeconds = from.revealDurationSeconds,
            )

        fun lerpSettings(from: BeautySettings, to: BeautySettings, t: Float): BeautySettings {
            val a = t.coerceIn(0f, 1f)
            fun L(x: Float, y: Float) = MathUtils.lerp(x, y, a)
            return from.copy(
                globalStrength = L(from.globalStrength, to.globalStrength),
                smoothingStrength = L(from.smoothingStrength, to.smoothingStrength),
                smoothingRadius = L(from.smoothingRadius, to.smoothingRadius),
                detailRetention = L(from.detailRetention, to.detailRetention),
                complexionEvenness = L(from.complexionEvenness, to.complexionEvenness),
                rednessCorrection = L(from.rednessCorrection, to.rednessCorrection),
                blemishControl = L(from.blemishControl, to.blemishControl),
                shineControl = L(from.shineControl, to.shineControl),
                skinGlow = L(from.skinGlow, to.skinGlow),
                underEyeStrength = L(from.underEyeStrength, to.underEyeStrength),
                underEyeSmoothing = L(from.underEyeSmoothing, to.underEyeSmoothing),
                underEyeMaximumLift = L(from.underEyeMaximumLift, to.underEyeMaximumLift),
                underEyeColorCorrection = L(from.underEyeColorCorrection, to.underEyeColorCorrection),
                faceExposure = L(from.faceExposure, to.faceExposure),
                shadowLift = L(from.shadowLift, to.shadowLift),
                highlightProtection = L(from.highlightProtection, to.highlightProtection),
                warmth = L(from.warmth, to.warmth),
                localContrast = L(from.localContrast, to.localContrast),
                contourStrength = L(from.contourStrength, to.contourStrength),
                blushStrength = L(from.blushStrength, to.blushStrength),
                eyeClarity = L(from.eyeClarity, to.eyeClarity),
                eyeBrightening = L(from.eyeBrightening, to.eyeBrightening),
                eyeSparkle = L(from.eyeSparkle, to.eyeSparkle),
                browDefinition = L(from.browDefinition, to.browDefinition),
                teethWhitening = L(from.teethWhitening, to.teethWhitening),
                lipEnhancement = L(from.lipEnhancement, to.lipEnhancement),
                lipTintStrength = L(from.lipTintStrength, to.lipTintStrength),
                lipDefinition = L(from.lipDefinition, to.lipDefinition),
                lipGloss = L(from.lipGloss, to.lipGloss),
                detailPreservation = L(from.detailPreservation, to.detailPreservation),
                faceSlimming = L(from.faceSlimming, to.faceSlimming),
                eyeEnlargement = L(from.eyeEnlargement, to.eyeEnlargement),
                noseRefinement = L(from.noseRefinement, to.noseRefinement),
                lakeIntensity = L(from.lakeIntensity, to.lakeIntensity),
                lakeMotion = L(from.lakeMotion, to.lakeMotion),
                lakeDarkness = L(from.lakeDarkness, to.lakeDarkness),
                lakeFaceClarity = L(from.lakeFaceClarity, to.lakeFaceClarity),
            )
        }
    }
}
