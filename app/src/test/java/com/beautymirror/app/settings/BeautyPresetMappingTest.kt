package com.beautymirror.app.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BeautyPresetMappingTest {
    @Test
    fun offDisablesEffects() {
        val s = BeautySettings.fromPreset(BeautyPreset.OFF)
        assertThat(s.effectsEnabled).isFalse()
        assertThat(s.globalStrength).isEqualTo(0f)
    }

    @Test
    fun naturalIsDefaultBaseline() {
        val n = BeautySettings.natural()
        assertThat(n.smoothingStrength).isLessThan(BeautySettings.soft().smoothingStrength)
        assertThat(n.faceExposure).isLessThan(BeautySettings.bright().faceExposure)
    }

    @Test
    fun customPreservesQualityAndDebug() {
        val base = BeautySettings.natural().copy(qualityLevel = QualityLevel.HIGH, debugOverlay = true)
        val custom = BeautySettings.fromGlobalStrength(0.7f, base)
        assertThat(custom.qualityLevel).isEqualTo(QualityLevel.HIGH)
        assertThat(custom.debugOverlay).isTrue()
        val viaPreset = BeautySettings.fromPreset(BeautyPreset.CUSTOM, base)
        assertThat(viaPreset.debugOverlay).isTrue()
        assertThat(viaPreset.qualityLevel).isEqualTo(QualityLevel.HIGH)
    }

    @Test
    fun maxGlobalStaysBounded() {
        val s = BeautySettings.fromGlobalStrength(1f).clamped()
        assertThat(s.smoothingStrength).isAtMost(0.88f)
        assertThat(s.underEyeStrength).isAtMost(0.72f)
        assertThat(s.faceExposure).isAtMost(0.08f)
        assertThat(s.globalStrength).isEqualTo(1f)
    }

    @Test
    fun fromPresetOffKeepsQualityAndDebug() {
        val cur = BeautySettings.natural().copy(qualityLevel = QualityLevel.HIGH, debugOverlay = true)
        val off = BeautySettings.fromPreset(BeautyPreset.OFF, cur)
        assertThat(off.preset).isEqualTo(BeautyPreset.OFF)
        assertThat(off.effectsEnabled).isFalse()
        assertThat(off.qualityLevel).isEqualTo(QualityLevel.HIGH)
        assertThat(off.debugOverlay).isTrue()
    }
}
