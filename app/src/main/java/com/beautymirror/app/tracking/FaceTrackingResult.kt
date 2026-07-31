package com.beautymirror.app.tracking

data class LandmarkPoint(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
)

data class FaceBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) * 0.5f
    val centerY: Float get() = (top + bottom) * 0.5f
}

/**
 * All landmark spaces are normalized [0,1] unless noted.
 * display*: preview surface space after rotation/mirror/crop.
 * texture*: OpenGL texture UV space (origin bottom-left convention handled by renderer).
 */
data class FaceTrackingResult(
    val timestampNs: Long,
    val bounds: FaceBounds,
    val normalizedLandmarks: List<LandmarkPoint>,
    val displayLandmarks: List<LandmarkPoint>,
    val textureLandmarks: List<LandmarkPoint>,
    val trackingConfidence: Float,
    val presenceConfidence: Float,
    val headYaw: Float = 0f,
    val headPitch: Float = 0f,
    val headRoll: Float = 0f,
    val isValid: Boolean,
    val timeSinceValidMs: Long,
    val state: TrackingState,
    val effectOpacity: Float,
    val faceLuminance: Float = 0.5f,
    val leftCheekColor: FloatArray = floatArrayOf(0.75f, 0.62f, 0.58f),
    val rightCheekColor: FloatArray = floatArrayOf(0.75f, 0.62f, 0.58f),
    /** Texture-space UV for cheek reference sampling (outside under-eye mask). */
    val leftCheekUv: LandmarkPoint = LandmarkPoint(0.35f, 0.55f),
    val rightCheekUv: LandmarkPoint = LandmarkPoint(0.65f, 0.55f),
) {
    companion object {
        fun empty(nowNs: Long = 0L): FaceTrackingResult = FaceTrackingResult(
            timestampNs = nowNs,
            bounds = FaceBounds(0f, 0f, 0f, 0f),
            normalizedLandmarks = emptyList(),
            displayLandmarks = emptyList(),
            textureLandmarks = emptyList(),
            trackingConfidence = 0f,
            presenceConfidence = 0f,
            isValid = false,
            timeSinceValidMs = Long.MAX_VALUE,
            state = TrackingState.NO_FACE,
            effectOpacity = 0f,
        )
    }
}
