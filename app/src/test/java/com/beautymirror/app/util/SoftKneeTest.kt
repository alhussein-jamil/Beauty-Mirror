package com.beautymirror.app.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SoftKneeTest {
    @Test
    fun softKneeMonotonic() {
        val a = MathUtils.softKnee(0.2f, 1f)
        val b = MathUtils.softKnee(0.8f, 1f)
        assertThat(b).isGreaterThan(a)
        assertThat(a).isGreaterThan(0f)
        assertThat(b).isLessThan(1.01f)
    }

    @Test
    fun effectAmountDeadzoneIsZero() {
        assertThat(MathUtils.effectAmount(0f)).isEqualTo(0f)
        assertThat(MathUtils.effectAmount(0.012f)).isEqualTo(0f)
        assertThat(MathUtils.effectAmount(-0.01f)).isEqualTo(0f)
        assertThat(MathUtils.effectAmount(0.04f)).isGreaterThan(0f)
    }

    @Test
    fun effectAmountMonotonicAboveDeadzone() {
        val low = MathUtils.effectAmount(0.1f)
        val mid = MathUtils.effectAmount(0.5f)
        val high = MathUtils.effectAmount(1f)
        assertThat(low).isGreaterThan(0f)
        assertThat(mid).isGreaterThan(low)
        assertThat(high).isGreaterThan(mid)
        assertThat(high).isAtMost(1f)
    }

    @Test
    fun effectAmountPreservesSign() {
        assertThat(MathUtils.effectAmount(-0.5f)).isLessThan(0f)
        assertThat(MathUtils.effectAmount(0.5f)).isGreaterThan(0f)
    }
}
