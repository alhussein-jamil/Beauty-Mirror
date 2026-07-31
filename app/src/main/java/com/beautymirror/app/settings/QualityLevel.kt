package com.beautymirror.app.settings

/**
 * Runtime quality profile.
 *
 * [PERFORMANCE] is intentionally hidden from the manual selector. The adaptive controller can
 * enter it when the device cannot sustain 30 FPS at LOW. It preserves the core skin, under-eye
 * and lighting corrections while removing optional geometry/detail/feature styling and cutting
 * mask/render resolution aggressively.
 */
enum class QualityLevel(
    val previewTargetHeight: Int,
    val maskResolution: Int,
    val analysisHz: Float,
    val smoothingSamples: Int,
    val secondMaskBlur: Boolean,
    val geometryEnabled: Boolean,
    val detailRestorationEnabled: Boolean,
    val featureEnhancementEnabled: Boolean,
    val userSelectable: Boolean,
) {
    PERFORMANCE(
        previewTargetHeight = 480,
        maskResolution = 128,
        analysisHz = 6f,
        smoothingSamples = 2,
        secondMaskBlur = false,
        geometryEnabled = false,
        detailRestorationEnabled = false,
        featureEnhancementEnabled = false,
        userSelectable = false,
    ),
    LOW(
        previewTargetHeight = 720,
        maskResolution = 224,
        analysisHz = 10f,
        smoothingSamples = 4,
        secondMaskBlur = false,
        geometryEnabled = true,
        // Keep eye clarity/sparkle alive at the default quality tier.
        detailRestorationEnabled = true,
        featureEnhancementEnabled = true,
        userSelectable = true,
    ),
    MEDIUM(
        previewTargetHeight = 900,
        maskResolution = 384,
        analysisHz = 15f,
        smoothingSamples = 7,
        // One mask blur is enough at exhibition distance; second pass was mostly edge polish.
        secondMaskBlur = false,
        geometryEnabled = true,
        detailRestorationEnabled = true,
        featureEnhancementEnabled = true,
        userSelectable = true,
    ),
    HIGH(
        previewTargetHeight = 1080,
        maskResolution = 512,
        analysisHz = 18f,
        smoothingSamples = 8,
        secondMaskBlur = true,
        geometryEnabled = true,
        detailRestorationEnabled = true,
        featureEnhancementEnabled = true,
        userSelectable = true,
    );

    companion object {
        val userChoices: List<QualityLevel> = entries.filter { it.userSelectable }

        fun fromName(name: String): QualityLevel =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?.takeIf { it.userSelectable }
                ?: MEDIUM
    }
}
