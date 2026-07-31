package com.beautymirror.app.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SettingsInterpolatorTest {
    @Test
    fun snapsUntilEffectsAppliedThenLerps() {
        val interp = SettingsInterpolator(lerpAlpha = 0.5f, dimPulse = 0.3f, dimDecay = 0.5f)
        val a = BeautySettings.natural().copy(smoothingStrength = 0.2f)
        val b = BeautySettings.natural().copy(smoothingStrength = 0.8f)

        interp.setTarget(a)
        assertThat(interp.tick().smoothingStrength).isEqualTo(0.2f)
        assertThat(interp.effectsReady()).isFalse()

        interp.setTarget(b)
        // Still cold — snap to latest target, no half-lerp.
        assertThat(interp.tick().smoothingStrength).isEqualTo(0.8f)

        interp.markEffectsApplied()
        assertThat(interp.effectsReady()).isTrue()

        interp.setTarget(a)
        val mid = interp.tick().smoothingStrength
        assertThat(mid).isGreaterThan(0.2f)
        assertThat(mid).isLessThan(0.8f)
        assertThat(interp.dimAmount()).isGreaterThan(0f)
    }

    @Test
    fun resetGateSnapsAgain() {
        val interp = SettingsInterpolator(lerpAlpha = 0.5f)
        interp.setTarget(BeautySettings.natural().copy(blushStrength = 0.4f))
        interp.markEffectsApplied()
        interp.setTarget(BeautySettings.natural().copy(blushStrength = 0.1f))
        interp.tick()
        interp.resetEffectsGate()
        interp.setTarget(BeautySettings.natural().copy(blushStrength = 0.9f))
        assertThat(interp.tick().blushStrength).isEqualTo(0.9f)
        assertThat(interp.effectsReady()).isFalse()
    }
    @Test
    fun interpolationIsFrameRateIndependent() {
        fun run(frameRate: Int): Float {
            val interp = SettingsInterpolator(lerpAlpha = 0.28f)
            interp.setTarget(BeautySettings.natural().copy(smoothingStrength = 0.1f))
            interp.markEffectsApplied()
            interp.setTarget(BeautySettings.natural().copy(smoothingStrength = 0.9f))
            repeat(frameRate) { interp.tick(1f / frameRate) }
            return interp.displayed().smoothingStrength
        }

        assertThat(run(60)).isWithin(0.002f).of(run(30))
        assertThat(run(24)).isWithin(0.002f).of(run(30))
    }

}
