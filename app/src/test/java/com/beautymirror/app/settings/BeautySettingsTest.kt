package com.beautymirror.app.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BeautySettingsTest {
    @Test
    fun clampBounds() {
        val s = BeautySettings(
            globalStrength = 2f,
            underEyeMaximumLift = 1f,
            faceExposure = 2f,
            lakeIntensity = 2f,
            lakeMotion = -1f,
            lakeDarkness = 3f,
            lakeFaceClarity = -2f,
        ).clamped()
        assertThat(s.globalStrength).isEqualTo(1f)
        assertThat(s.underEyeMaximumLift).isEqualTo(0.4f)
        assertThat(s.faceExposure).isEqualTo(0.5f)
        assertThat(s.lakeIntensity).isEqualTo(1f)
        assertThat(s.lakeMotion).isEqualTo(0f)
        assertThat(s.lakeDarkness).isEqualTo(1f)
        assertThat(s.lakeFaceClarity).isEqualTo(0f)
    }

    @Test
    fun presets() {
        assertThat(BeautySettings.off().effectsEnabled).isFalse()
        assertThat(BeautySettings.natural().preset).isEqualTo(BeautyPreset.NATURAL)
        assertThat(BeautySettings.soft().smoothingStrength)
            .isGreaterThan(BeautySettings.natural().smoothingStrength)
        assertThat(BeautySettings.bright().faceExposure)
            .isGreaterThan(BeautySettings.natural().faceExposure)
        assertThat(BeautySettings.stage().qualityLevel).isEqualTo(QualityLevel.HIGH)
        assertThat(BeautySettings.glam().faceSlimming)
            .isGreaterThan(BeautySettings.natural().faceSlimming)
    }

    @Test
    fun globalStrengthMapping() {
        val s = BeautySettings.fromGlobalStrength(1f)
        assertThat(s.smoothingStrength).isWithin(1e-3f).of(0.94f)
        assertThat(s.underEyeStrength).isWithin(1e-3f).of(0.84f)
        assertThat(s.faceExposure).isWithin(1e-3f).of(0.09f)
    }

    @Test
    fun interpolateOffAtZero() {
        val s = BeautySettings.fromGlobalStrength(0f)
        assertThat(s.preset).isEqualTo(BeautyPreset.OFF)
    }

    @Test
    fun clampCapsUnderEyeLift() {
        val s = BeautySettings.natural().copy(underEyeMaximumLift = 0.9f).clamped()
        assertThat(s.underEyeMaximumLift).isAtMost(0.4f)
    }


    @Test
    fun naturalMatchesConstructorDefaults() {
        val n = BeautySettings.natural()
        val d = BeautySettings()
        assertThat(n.globalStrength).isWithin(1e-4f).of(d.globalStrength)
        assertThat(n.smoothingStrength).isWithin(1e-4f).of(d.smoothingStrength)
        assertThat(n.underEyeStrength).isWithin(1e-4f).of(d.underEyeStrength)
        assertThat(n.detailRetention).isWithin(1e-4f).of(d.detailRetention)
    }

    @Test
    fun underEyeColorOrLiftAloneEnablesEffects() {
        assertThat(
            BeautySettings.off().copy(
                preset = BeautyPreset.CUSTOM,
                underEyeColorCorrection = 0.5f,
            ).effectsEnabled,
        ).isTrue()
        assertThat(
            BeautySettings.off().copy(
                preset = BeautyPreset.CUSTOM,
                underEyeMaximumLift = 0.2f,
            ).effectsEnabled,
        ).isTrue()
    }

    @Test
    fun fromGlobalStrengthScalesUnderEyeAux() {
        val s = BeautySettings.fromGlobalStrength(1f)
        assertThat(s.underEyeMaximumLift).isWithin(1e-3f).of(0.22f)
        assertThat(s.underEyeColorCorrection).isWithin(1e-3f).of(0.60f)
    }

    @Test
    fun presetsPreserveReflectionScene() {
        val current = BeautySettings.natural().copy(
            reflectionScene = ReflectionScene.DARK_LAKE,
            lakeIntensity = 0.73f,
            lakeMotion = 0.21f,
            lakeDarkness = 0.64f,
            lakeFaceClarity = 0.84f,
        )
        val stage = BeautySettings.fromPreset(BeautyPreset.STAGE, current)
        assertThat(stage.reflectionScene).isEqualTo(ReflectionScene.DARK_LAKE)
        assertThat(stage.lakeIntensity).isWithin(1e-4f).of(0.73f)
        assertThat(stage.lakeMotion).isWithin(1e-4f).of(0.21f)
        assertThat(stage.lakeDarkness).isWithin(1e-4f).of(0.64f)
        assertThat(stage.lakeFaceClarity).isWithin(1e-4f).of(0.84f)
    }

}
