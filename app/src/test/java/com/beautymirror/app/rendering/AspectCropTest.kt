package com.beautymirror.app.rendering

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AspectCropTest {
    @Test
    fun equalAspectDoesNotCrop() {
        val crop = AspectCrop.centerCrop(1920, 1080, 1280, 720)
        assertThat(crop.scaleX).isEqualTo(1f)
        assertThat(crop.scaleY).isEqualTo(1f)
    }

    @Test
    fun widerSourceCropsHorizontally() {
        val crop = AspectCrop.centerCrop(1920, 1080, 1080, 1920)
        assertThat(crop.scaleX).isLessThan(1f)
        assertThat(crop.scaleY).isEqualTo(1f)
    }

    @Test
    fun tallerSourceCropsVertically() {
        val crop = AspectCrop.centerCrop(1080, 1920, 1920, 1080)
        assertThat(crop.scaleX).isEqualTo(1f)
        assertThat(crop.scaleY).isLessThan(1f)
    }

    @Test
    fun portraitDisplayUsesRotatedCameraDimensions() {
        val crop = AspectCrop.centerCrop(1920, 1080, 1080, 2400, rotationDegrees = 90)
        assertThat(crop.scaleX).isLessThan(1f)
        assertThat(crop.scaleY).isEqualTo(1f)
        assertThat(crop.scaleX).isGreaterThan(0.75f)
    }

    @Test
    fun rotationIsNormalized() {
        val a = AspectCrop.centerCrop(1920, 1080, 1080, 2400, rotationDegrees = 90)
        val b = AspectCrop.centerCrop(1920, 1080, 1080, 2400, rotationDegrees = 450)
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun invalidDimensionsRemainFinite() {
        val crop = AspectCrop.centerCrop(0, -1, 0, -5)
        assertThat(crop.scaleX).isWithin(1e-6f).of(1f)
        assertThat(crop.scaleY).isWithin(1e-6f).of(1f)
    }
}
