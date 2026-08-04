package com.beautymirror.app.settings

import com.beautymirror.app.tracking.FaceBounds
import com.beautymirror.app.tracking.FaceTrackingResult
import com.beautymirror.app.tracking.TrackingState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VisitorSessionDetectorTest {
    @Test
    fun firstFaceStartsSessionOnlyOnce() {
        val detector = VisitorSessionDetector()
        assertThat(detector.update(face(), 1f / 30f)).isTrue()
        assertThat(detector.update(face(), 1f / 30f)).isFalse()
    }

    @Test
    fun briefMissDoesNotCreateNewVisitor() {
        val detector = VisitorSessionDetector(rearmAfterSeconds = 0.65f)
        detector.update(face(), 1f / 30f)
        repeat(6) { detector.update(FaceTrackingResult.empty(), 1f / 30f) }
        assertThat(detector.update(face(), 1f / 30f)).isFalse()
    }

    @Test
    fun sustainedAbsenceRearmsNextVisitor() {
        val detector = VisitorSessionDetector(rearmAfterSeconds = 0.65f)
        detector.update(face(), 1f / 30f)
        repeat(24) { detector.update(FaceTrackingResult.empty(), 1f / 30f) }
        assertThat(detector.update(face(), 1f / 30f)).isTrue()
    }


    @Test
    fun directTrackingHandoffRestartsAfterTwoDiscontinuousFrames() {
        val detector = VisitorSessionDetector()
        detector.update(face(), 1f / 30f)
        val replacement = face().copy(
            bounds = FaceBounds(0.68f, 0.22f, 0.94f, 0.72f),
        )
        assertThat(detector.update(replacement, 1f / 30f)).isFalse()
        assertThat(detector.update(replacement, 1f / 30f)).isTrue()
        assertThat(detector.update(replacement, 1f / 30f)).isFalse()
    }

    @Test
    fun ordinaryFaceMovementDoesNotRestart() {
        val detector = VisitorSessionDetector()
        detector.update(face(), 1f / 30f)
        val moved = face().copy(
            bounds = FaceBounds(0.30f, 0.20f, 0.80f, 0.88f),
        )
        repeat(4) {
            assertThat(detector.update(moved, 1f / 30f)).isFalse()
        }
    }

    private fun face(): FaceTrackingResult = FaceTrackingResult.empty().copy(
        bounds = FaceBounds(0.25f, 0.18f, 0.75f, 0.86f),
        isValid = true,
        effectOpacity = 1f,
        trackingConfidence = 1f,
        presenceConfidence = 1f,
        state = TrackingState.TRACKING,
    )
}
