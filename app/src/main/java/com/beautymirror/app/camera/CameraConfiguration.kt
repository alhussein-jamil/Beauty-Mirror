package com.beautymirror.app.camera

import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import com.beautymirror.app.settings.QualityLevel

data class CameraConfiguration(
    val lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    val aspectRatio: Int = AspectRatio.RATIO_16_9,
    val qualityLevel: QualityLevel = QualityLevel.MEDIUM,
) {
    val isFront: Boolean get() = lensFacing == CameraSelector.LENS_FACING_FRONT
}
