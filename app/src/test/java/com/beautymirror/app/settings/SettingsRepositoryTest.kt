package com.beautymirror.app.settings

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SettingsRepositoryTest {
    @Test
    fun customAdvancedFieldsRoundTrip() = runTest {
        val repo = SettingsRepository(RuntimeEnvironment.getApplication())
        val custom = BeautySettings.natural().copy(
            preset = BeautyPreset.CUSTOM,
            globalStrength = 0.55f,
            smoothingStrength = 0.33f,
            smoothingRadius = 3.1f,
            detailRetention = 0.7f,
            complexionEvenness = 0.42f,
            rednessCorrection = 0.31f,
            blemishControl = 0.37f,
            shineControl = 0.29f,
            skinGlow = 0.21f,
            underEyeSmoothing = 0.44f,
            underEyeMaximumLift = 0.2f,
            underEyeColorCorrection = 0.4f,
            highlightProtection = 0.5f,
            eyeSparkle = 0.35f,
            browDefinition = 0.28f,
            detailPreservation = 0.6f,
            qualityLevel = QualityLevel.HIGH,
            debugOverlay = true,
            reflectionScene = ReflectionScene.DARK_LAKE,
            lakeIntensity = 0.71f,
            lakeMotion = 0.27f,
            lakeDarkness = 0.59f,
            lakeFaceClarity = 0.83f,
        ).clamped()
        repo.save(custom)
        val loaded = repo.settingsFlow.first()
        assertThat(loaded.preset).isEqualTo(BeautyPreset.CUSTOM)
        assertThat(loaded.smoothingRadius).isWithin(1e-3f).of(3.1f)
        assertThat(loaded.detailRetention).isWithin(1e-3f).of(0.7f)
        assertThat(loaded.complexionEvenness).isWithin(1e-3f).of(0.42f)
        assertThat(loaded.rednessCorrection).isWithin(1e-3f).of(0.31f)
        assertThat(loaded.blemishControl).isWithin(1e-3f).of(0.37f)
        assertThat(loaded.shineControl).isWithin(1e-3f).of(0.29f)
        assertThat(loaded.skinGlow).isWithin(1e-3f).of(0.21f)
        assertThat(loaded.underEyeSmoothing).isWithin(1e-3f).of(0.44f)
        assertThat(loaded.eyeSparkle).isWithin(1e-3f).of(0.35f)
        assertThat(loaded.browDefinition).isWithin(1e-3f).of(0.28f)
        assertThat(loaded.underEyeMaximumLift).isWithin(1e-3f).of(0.2f)
        assertThat(loaded.qualityLevel).isEqualTo(QualityLevel.HIGH)
        assertThat(loaded.debugOverlay).isTrue()
        assertThat(loaded.reflectionScene).isEqualTo(ReflectionScene.DARK_LAKE)
        assertThat(loaded.lakeIntensity).isWithin(1e-3f).of(0.71f)
        assertThat(loaded.lakeMotion).isWithin(1e-3f).of(0.27f)
        assertThat(loaded.lakeDarkness).isWithin(1e-3f).of(0.59f)
        assertThat(loaded.lakeFaceClarity).isWithin(1e-3f).of(0.83f)
    }
}
