package com.beautymirror.app.tracking

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FaceBoundsTest {
    @Test
    fun widthHeightCenter() {
        val b = FaceBounds(0.2f, 0.1f, 0.8f, 0.9f)
        assertThat(b.width).isWithin(1e-4f).of(0.6f)
        assertThat(b.height).isWithin(1e-4f).of(0.8f)
        assertThat(b.centerX).isWithin(1e-4f).of(0.5f)
        assertThat(b.centerY).isWithin(1e-4f).of(0.5f)
    }
}
