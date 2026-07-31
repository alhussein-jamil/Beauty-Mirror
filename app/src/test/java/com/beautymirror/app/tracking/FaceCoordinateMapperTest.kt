package com.beautymirror.app.tracking

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FaceCoordinateMapperTest {
    private fun squareConfig(mirror: Boolean = true) = FaceCoordinateMapper.Config(
        rotationDegrees = 0,
        mirrorFront = mirror,
        analysisWidth = 100,
        analysisHeight = 100,
        previewWidth = 100,
        previewHeight = 100,
    )

    @Test
    fun mirrorPresentationMapsDisplayAndTextureTogether() {
        val mapper = FaceCoordinateMapper().apply { config = squareConfig(mirror = true) }
        val display = mapper.toDisplay(0.2f, 0.3f)
        val texture = mapper.toTexture(0.2f, 0.3f)
        assertThat(display.x).isWithin(1e-4f).of(0.8f)
        assertThat(texture.x).isWithin(1e-4f).of(0.8f)
        assertThat(texture.y).isWithin(1e-4f).of(1f - display.y)
    }

    @Test
    fun unmirroredPresentationKeepsInputX() {
        val mapper = FaceCoordinateMapper().apply { config = squareConfig(mirror = false) }
        val display = mapper.toDisplay(0.2f, 0.3f)
        val texture = mapper.toTexture(0.2f, 0.3f)
        assertThat(display.x).isWithin(1e-4f).of(0.2f)
        assertThat(texture.x).isWithin(1e-4f).of(0.2f)
        assertThat(texture.y).isWithin(1e-4f).of(0.7f)
    }

    @Test
    fun mapBoundsSurvivesMirror() {
        val mapper = FaceCoordinateMapper().apply { config = squareConfig(mirror = true) }
        val bounds = mapper.mapBounds(0.2f, 0.2f, 0.4f, 0.5f)
        assertThat(bounds.left).isWithin(1e-4f).of(0.6f)
        assertThat(bounds.right).isWithin(1e-4f).of(0.8f)
        assertThat(bounds.top).isLessThan(bounds.bottom)
    }

    @Test
    fun uprightMediaPipePathSkipsSecondRotation() {
        val mapper = FaceCoordinateMapper()
        mapper.config = FaceCoordinateMapper.Config(
            rotationDegrees = 0,
            mirrorFront = false,
            analysisWidth = 480,
            analysisHeight = 640,
            previewWidth = 1080,
            previewHeight = 1920,
        )
        assertThat(mapper.toAligned(0.25f, 0.5f).x).isLessThan(0.5f)
    }

    @Test
    fun bitmapFallbackPathAppliesRotation() {
        val mapper = FaceCoordinateMapper()
        mapper.config = FaceCoordinateMapper.Config(
            rotationDegrees = 90,
            mirrorFront = false,
            analysisWidth = 640,
            analysisHeight = 480,
            previewWidth = 480,
            previewHeight = 640,
        )
        val point = mapper.toAligned(0f, 0f)
        assertThat(point.x).isWithin(0.2f).of(1f)
        assertThat(point.y).isWithin(0.2f).of(0f)
    }

    @Test
    fun cropConfigMapsCropCenterToPreviewCenter() {
        val mapper = FaceCoordinateMapper()
        val config = squareConfig(mirror = false).copy(
            cropLeft = 0.25f,
            cropTop = 0.25f,
            cropWidth = 0.5f,
            cropHeight = 0.5f,
        )
        val center = mapper.toAligned(0.5f, 0.5f, config)
        val corner = mapper.toAligned(0.25f, 0.25f, config)
        assertThat(center.x).isWithin(1e-4f).of(0.5f)
        assertThat(center.y).isWithin(1e-4f).of(0.5f)
        assertThat(corner.x).isWithin(1e-4f).of(0f)
        assertThat(corner.y).isWithin(1e-4f).of(0f)
    }

    @Test
    fun mapLandmarksIntoMatchesAllocatingApi() {
        val mapper = FaceCoordinateMapper().apply { config = squareConfig(mirror = true) }
        val points = listOf(LandmarkPoint(0.1f, 0.2f), LandmarkPoint(0.8f, 0.7f))
        val (display, texture) = mapper.mapLandmarks(points)
        val displayOut = ArrayList<LandmarkPoint>()
        val textureOut = ArrayList<LandmarkPoint>()
        mapper.mapLandmarksInto(points, displayOut, textureOut)
        assertThat(displayOut).containsExactlyElementsIn(display).inOrder()
        assertThat(textureOut).containsExactlyElementsIn(texture).inOrder()
    }
}
