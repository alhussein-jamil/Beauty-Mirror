package com.beautymirror.app.rendering

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MirrorTransformTest {
    @Test
    fun extraFlipWhenUpstreamAlreadyMirrors() {
        assertThat(MirrorTransform.extraFlip(true, true, true)).isFalse()
        assertThat(MirrorTransform.extraFlip(false, true, true)).isTrue()
    }

    @Test
    fun extraFlipWhenUpstreamDoesNotMirror() {
        assertThat(MirrorTransform.extraFlip(true, true, false)).isTrue()
        assertThat(MirrorTransform.extraFlip(true, false, true)).isTrue()
        assertThat(MirrorTransform.extraFlip(false, false, false)).isFalse()
    }
}
