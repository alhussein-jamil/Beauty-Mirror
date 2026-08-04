package com.beautymirror.app.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VisitorRevealControllerTest {
    @Test
    fun tenSecondRevealIsHalfwayAtFiveSecondsAndCompleteAtTen() {
        val reveal = VisitorRevealController(durationSeconds = 10f)
        repeat(150) { reveal.update(targetPresence = 1f, deltaSeconds = 1f / 30f) }
        assertThat(reveal.value).isWithin(0.04f).of(0.5f)
        repeat(150) { reveal.update(targetPresence = 1f, deltaSeconds = 1f / 30f) }
        assertThat(reveal.value).isGreaterThan(0.995f)
    }

    @Test
    fun restartReturnsProgressToZero() {
        val reveal = VisitorRevealController(durationSeconds = 6f)
        repeat(90) { reveal.update(targetPresence = 1f, deltaSeconds = 1f / 30f) }
        assertThat(reveal.value).isGreaterThan(0.4f)
        reveal.restart()
        assertThat(reveal.value).isEqualTo(0f)
    }

    @Test
    fun releaseDoesNotSnapOnSingleMiss() {
        val reveal = VisitorRevealController()
        reveal.reset(1f)
        val afterOneMiss = reveal.update(targetPresence = 0f, deltaSeconds = 1f / 30f)
        assertThat(afterOneMiss).isGreaterThan(0.80f)
    }

    @Test
    fun largeFrameGapIsCapped() {
        val reveal = VisitorRevealController(durationSeconds = 10f)
        val value = reveal.update(targetPresence = 1f, deltaSeconds = 10f)
        assertThat(value).isLessThan(0.01f)
    }

    @Test
    fun durationIsClampedToWorkshopRange() {
        val reveal = VisitorRevealController(durationSeconds = 1f)
        assertThat(reveal.durationSeconds).isEqualTo(VisitorRevealController.MIN_DURATION_SECONDS)
        reveal.setDuration(90f)
        assertThat(reveal.durationSeconds).isEqualTo(VisitorRevealController.MAX_DURATION_SECONDS)
    }
}
