package com.beautymirror.app.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BeautyEffectsEnabledTest {
    @Test
    fun naturalEnablesEffects() {
        assertThat(BeautySettings.natural().effectsEnabled).isTrue()
    }

    @Test
    fun beforeAfterDisablesEffects() {
        assertThat(BeautySettings.natural().copy(showBeforeAfter = true).effectsEnabled).isFalse()
    }

    @Test
    fun offPresetDisablesEffects() {
        assertThat(BeautySettings.off().effectsEnabled).isFalse()
    }

    @Test
    fun customAdvancedEffectDoesNotDependOnGlobalSlider() {
        val custom = BeautySettings.off().copy(
            preset = BeautyPreset.CUSTOM,
            globalStrength = 0f,
            underEyeStrength = 0.3f,
        )
        assertThat(custom.effectsEnabled).isTrue()
    }

    @Test
    fun newTargetedEffectsEnablePipelineIndependently() {
        val custom = BeautySettings.off().copy(
            preset = BeautyPreset.CUSTOM,
            underEyeSmoothing = 0.4f,
            eyeSparkle = 0.2f,
            browDefinition = 0.2f,
        )
        assertThat(custom.effectsEnabled).isTrue()
    }

    @Test
    fun customWithAllCorrectionsAtZeroIsIdentity() {
        val custom = BeautySettings.off().copy(preset = BeautyPreset.CUSTOM)
        assertThat(custom.effectsEnabled).isFalse()
    }
}
