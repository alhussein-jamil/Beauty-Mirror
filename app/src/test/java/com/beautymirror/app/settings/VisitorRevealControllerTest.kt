package com.beautymirror.app.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VisitorRevealControllerTest {
    @Test
    fun revealIsNearlyCompleteWithinOneSecond() {
        val reveal = VisitorRevealController(attackSeconds = 0.9f, releaseSeconds = 1.35f)
        repeat(30) { reveal.update(targetPresence = 1f, deltaSeconds = 1f / 30f) }
        assertThat(reveal.value).isGreaterThan(0.98f)
    }

    @Test
    fun releaseIsSlowerAndDoesNotSnapOnSingleMiss() {
        val reveal = VisitorRevealController()
        reveal.reset(1f)
        val afterOneMiss = reveal.update(targetPresence = 0f, deltaSeconds = 1f / 30f)
        assertThat(afterOneMiss).isGreaterThan(0.85f)
    }

    @Test
    fun largeFrameGapIsCapped() {
        val reveal = VisitorRevealController()
        val value = reveal.update(targetPresence = 1f, deltaSeconds = 10f)
        assertThat(value).isLessThan(0.5f)
    }
}
