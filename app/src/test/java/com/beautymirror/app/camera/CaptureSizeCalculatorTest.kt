package com.beautymirror.app.camera

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CaptureSizeCalculatorTest {
    @Test fun portraitIsCappedWithoutAspectDistortion() {
        assertThat(CaptureSizeCalculator.fitWithin(1080, 2400)).isEqualTo(CaptureSize(864, 1920))
    }

    @Test fun landscapeIsCappedWithoutForcedPortraitMinimums() {
        assertThat(CaptureSizeCalculator.fitWithin(2560, 1440)).isEqualTo(CaptureSize(1920, 1080))
    }

    @Test fun smallerFramesAreNotUpscaled() {
        assertThat(CaptureSizeCalculator.fitWithin(720, 1280)).isEqualTo(CaptureSize(720, 1280))
    }
}
