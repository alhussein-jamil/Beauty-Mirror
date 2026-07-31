package com.beautymirror.app.tracking

import com.beautymirror.app.util.MathUtils
import kotlin.math.sqrt

/**
 * Adaptive exponential smoother for landmarks and scalar face metrics.
 */
class LandmarkSmoother(
    private val baseAlpha: Float = 0.35f,
    private val holdTimeoutMs: Long = 320L,
    private val fadeDurationMs: Long = 380L,
) {
    private var previous: List<LandmarkPoint> = emptyList()
    private var previousBounds: FaceBounds? = null
    private var lastValidAtMs: Long = 0L
    private var hadFace: Boolean = false
    private var smoothedLuma: Float = 0.5f
    private var smoothedYaw: Float = 0f
    private var smoothedPitch: Float = 0f
    private var smoothedRoll: Float = 0f
    private var leftCheek = floatArrayOf(0.75f, 0.62f, 0.58f)
    private var rightCheek = floatArrayOf(0.75f, 0.62f, 0.58f)
    private var leftCheekUv = LandmarkPoint(0.35f, 0.55f)
    private var rightCheekUv = LandmarkPoint(0.65f, 0.55f)
    private var smoothedOpacity: Float = 0f
    private val bufferA = ArrayList<LandmarkPoint>(478)
    private val bufferB = ArrayList<LandmarkPoint>(478)
    private var useA = true

    fun reset() {
        previous = emptyList()
        previousBounds = null
        lastValidAtMs = 0L
        hadFace = false
        smoothedOpacity = 0f
        smoothedLuma = 0.5f
        smoothedYaw = 0f
        smoothedPitch = 0f
        smoothedRoll = 0f
        leftCheek = floatArrayOf(0.75f, 0.62f, 0.58f)
        rightCheek = floatArrayOf(0.75f, 0.62f, 0.58f)
        leftCheekUv = LandmarkPoint(0.35f, 0.55f)
        rightCheekUv = LandmarkPoint(0.65f, 0.55f)
        bufferA.clear()
        bufferB.clear()
        useA = true
    }

    fun push(
        nowMs: Long,
        raw: FaceTrackingResult?,
    ): FaceTrackingResult {
        if (raw == null || !raw.isValid || raw.normalizedLandmarks.isEmpty()) {
            return onMissing(nowMs)
        }

        val velocity = estimateVelocity(raw.normalizedLandmarks)
        val alpha = MathUtils.adaptiveAlpha(baseAlpha, velocity, slowThreshold = 0.002f, fastThreshold = 0.04f)

        // Scratch into alternating buffers for previous[]; publish a copy (cross-thread safe).
        val dest = if (useA) bufferA else bufferB
        useA = !useA
        dest.clear()
        if (previous.size == raw.normalizedLandmarks.size) {
            for (i in raw.normalizedLandmarks.indices) {
                val p = raw.normalizedLandmarks[i]
                dest.add(
                    LandmarkPoint(
                        x = MathUtils.expSmooth(previous[i].x, p.x, alpha),
                        y = MathUtils.expSmooth(previous[i].y, p.y, alpha),
                        z = MathUtils.expSmooth(previous[i].z, p.z, alpha),
                    ),
                )
            }
        } else {
            dest.addAll(raw.normalizedLandmarks)
        }
        previous = dest
        val smoothedLandmarks = ArrayList(dest)

        val sb = previousBounds
        val bounds = if (sb != null) {
            FaceBounds(
                left = MathUtils.expSmooth(sb.left, raw.bounds.left, alpha),
                top = MathUtils.expSmooth(sb.top, raw.bounds.top, alpha),
                right = MathUtils.expSmooth(sb.right, raw.bounds.right, alpha),
                bottom = MathUtils.expSmooth(sb.bottom, raw.bounds.bottom, alpha),
            )
        } else raw.bounds
        previousBounds = bounds

        smoothedLuma = MathUtils.expSmooth(smoothedLuma, raw.faceLuminance, alpha * 0.6f)
        smoothedYaw = MathUtils.expSmooth(smoothedYaw, raw.headYaw, alpha * 0.55f)
        smoothedPitch = MathUtils.expSmooth(smoothedPitch, raw.headPitch, alpha * 0.55f)
        smoothedRoll = smoothAngle(smoothedRoll, raw.headRoll, alpha * 0.65f)
        for (i in 0..2) {
            leftCheek[i] = MathUtils.expSmooth(leftCheek[i], raw.leftCheekColor[i], alpha * 0.5f)
            rightCheek[i] = MathUtils.expSmooth(rightCheek[i], raw.rightCheekColor[i], alpha * 0.5f)
        }
        leftCheekUv = LandmarkPoint(
            MathUtils.expSmooth(leftCheekUv.x, raw.leftCheekUv.x, alpha * 0.45f),
            MathUtils.expSmooth(leftCheekUv.y, raw.leftCheekUv.y, alpha * 0.45f),
        )
        rightCheekUv = LandmarkPoint(
            MathUtils.expSmooth(rightCheekUv.x, raw.rightCheekUv.x, alpha * 0.45f),
            MathUtils.expSmooth(rightCheekUv.y, raw.rightCheekUv.y, alpha * 0.45f),
        )

        lastValidAtMs = nowMs
        hadFace = true
        // Snappier lock-on so effects reach full strength quickly after the first solid mesh.
        val opacityAlpha = if (smoothedOpacity < 0.85f) 0.55f else 0.35f
        smoothedOpacity = MathUtils.expSmooth(smoothedOpacity, 1f, opacityAlpha)

        return raw.copy(
            normalizedLandmarks = smoothedLandmarks,
            bounds = bounds,
            isValid = true,
            timeSinceValidMs = 0L,
            state = TrackingState.TRACKING,
            effectOpacity = smoothedOpacity,
            headYaw = smoothedYaw,
            headPitch = smoothedPitch,
            headRoll = smoothedRoll,
            faceLuminance = smoothedLuma,
            leftCheekColor = leftCheek.copyOf(),
            rightCheekColor = rightCheek.copyOf(),
            leftCheekUv = leftCheekUv,
            rightCheekUv = rightCheekUv,
        )
    }

    private fun onMissing(nowMs: Long): FaceTrackingResult {
        if (!hadFace || previous.isEmpty()) {
            smoothedOpacity = MathUtils.expSmooth(smoothedOpacity, 0f, 0.35f)
            return FaceTrackingResult.empty().copy(effectOpacity = smoothedOpacity, state = TrackingState.NO_FACE)
        }
        val dt = nowMs - lastValidAtMs
        return when {
            dt <= holdTimeoutMs -> {
                smoothedOpacity = MathUtils.expSmooth(smoothedOpacity, 1f, 0.2f)
                FaceTrackingResult(
                    timestampNs = nowMs * 1_000_000L,
                    bounds = previousBounds ?: FaceBounds(0f, 0f, 0f, 0f),
                    normalizedLandmarks = ArrayList(previous),
                    displayLandmarks = emptyList(),
                    textureLandmarks = emptyList(),
                    trackingConfidence = 0.4f,
                    presenceConfidence = 0.4f,
                    isValid = true,
                    timeSinceValidMs = dt,
                    state = TrackingState.HOLDING,
                    effectOpacity = smoothedOpacity,
                    headYaw = smoothedYaw,
                    headPitch = smoothedPitch,
                    headRoll = smoothedRoll,
                    faceLuminance = smoothedLuma,
                    leftCheekColor = leftCheek.copyOf(),
                    rightCheekColor = rightCheek.copyOf(),
                    leftCheekUv = leftCheekUv,
                    rightCheekUv = rightCheekUv,
                )
            }
            dt <= holdTimeoutMs + fadeDurationMs -> {
                val t = ((dt - holdTimeoutMs).toFloat() / fadeDurationMs).coerceIn(0f, 1f)
                smoothedOpacity = (1f - t)
                FaceTrackingResult(
                    timestampNs = nowMs * 1_000_000L,
                    bounds = previousBounds ?: FaceBounds(0f, 0f, 0f, 0f),
                    normalizedLandmarks = ArrayList(previous),
                    displayLandmarks = emptyList(),
                    textureLandmarks = emptyList(),
                    trackingConfidence = 0.2f,
                    presenceConfidence = 0.2f,
                    isValid = false,
                    timeSinceValidMs = dt,
                    state = TrackingState.FADING,
                    effectOpacity = smoothedOpacity,
                    headYaw = smoothedYaw,
                    headPitch = smoothedPitch,
                    headRoll = smoothedRoll,
                    faceLuminance = smoothedLuma,
                    leftCheekColor = leftCheek.copyOf(),
                    rightCheekColor = rightCheek.copyOf(),
                    leftCheekUv = leftCheekUv,
                    rightCheekUv = rightCheekUv,
                )
            }
            else -> {
                reset()
                FaceTrackingResult.empty().copy(state = TrackingState.NO_FACE, effectOpacity = 0f)
            }
        }
    }

    private fun estimateVelocity(points: List<LandmarkPoint>): Float {
        if (previous.size != points.size || points.isEmpty()) return 1f
        var sum = 0f
        // Stable mid-face landmarks (cheeks / nose / chin / brow) when mesh is present.
        val sample = STABLE_VELOCITY_INDICES
        if (points.size > sample.last()) {
            for (i in sample) {
                val dx = points[i].x - previous[i].x
                val dy = points[i].y - previous[i].y
                sum += sqrt(dx * dx + dy * dy)
            }
            return sum / sample.size
        }
        val n = minOf(points.size, 40)
        for (i in 0 until n) {
            val dx = points[i].x - previous[i].x
            val dy = points[i].y - previous[i].y
            sum += sqrt(dx * dx + dy * dy)
        }
        return sum / n
    }


    private fun smoothAngle(previous: Float, current: Float, alpha: Float): Float {
        var delta = current - previous
        while (delta > 180f) delta -= 360f
        while (delta < -180f) delta += 360f
        var value = previous + delta * alpha.coerceIn(0f, 1f)
        while (value > 180f) value -= 360f
        while (value < -180f) value += 360f
        return value
    }

    companion object {
        // MediaPipe face mesh: left cheek, right cheek, nose, chin, brow center-ish.
        private val STABLE_VELOCITY_INDICES = intArrayOf(50, 280, 1, 152, 10, 338, 67)
    }
}
