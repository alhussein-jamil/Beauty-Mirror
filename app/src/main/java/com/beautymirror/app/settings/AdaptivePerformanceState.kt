package com.beautymirror.app.settings

/**
 * Continuously interpolated render budget.
 *
 * Resolution changes still happen through [QualityLevel], but these values remove optional work
 * gradually before a costly camera rebind is needed.
 */
data class AdaptivePerformanceState(
    val pressure: Float = 0f,
    val sampleScale: Float = 1f,
    val optionalScale: Float = 1f,
    val maskRefreshIntervalMs: Long = 0L,
    val protecting: Boolean = false,
    val cameraLimited: Boolean = false,
) {
    companion object {
        val FULL = AdaptivePerformanceState()
    }
}
