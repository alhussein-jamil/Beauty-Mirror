package com.beautymirror.app.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MathUtilsTest {
    @Test
    fun expSmooth() {
        val v = MathUtils.expSmooth(0f, 1f, 0.25f)
        assertThat(v).isWithin(1e-4f).of(0.25f)
    }

    @Test
    fun adaptiveAlphaIncreasesWithVelocity() {
        val slow = MathUtils.adaptiveAlpha(0.4f, 0f, 0.002f, 0.04f)
        val fast = MathUtils.adaptiveAlpha(0.4f, 0.05f, 0.002f, 0.04f)
        assertThat(fast).isGreaterThan(slow)
    }
}
