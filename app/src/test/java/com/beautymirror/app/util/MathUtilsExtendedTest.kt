package com.beautymirror.app.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MathUtilsExtendedTest {
    @Test
    fun adaptiveAlphaSlowsWhenStill() {
        val a = MathUtils.adaptiveAlpha(0.35f, velocity = 0.0005f, slowThreshold = 0.002f, fastThreshold = 0.04f)
        assertThat(a).isLessThan(0.35f)
    }

    @Test
    fun adaptiveAlphaSpeedsWhenFast() {
        val a = MathUtils.adaptiveAlpha(0.35f, velocity = 0.08f, slowThreshold = 0.002f, fastThreshold = 0.04f)
        assertThat(a).isGreaterThan(0.35f)
    }

    @Test
    fun expSmoothIdentityWhenAlphaOne() {
        assertThat(MathUtils.expSmooth(0f, 1f, 1f)).isWithin(1e-5f).of(1f)
    }

    @Test
    fun expSmoothHoldsWhenAlphaZero() {
        assertThat(MathUtils.expSmooth(0.4f, 1f, 0f)).isWithin(1e-5f).of(0.4f)
    }

    @Test
    fun clampWorks() {
        assertThat(MathUtils.clamp(2f, 0f, 1f)).isEqualTo(1f)
        assertThat(MathUtils.clamp(-1f, 0f, 1f)).isEqualTo(0f)
    }
}
