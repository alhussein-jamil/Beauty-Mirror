package com.beautymirror.app.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QualityLevelTest {
    @Test
    fun fromNameDefaultsUnknown() {
        assertThat(QualityLevel.fromName("nope")).isEqualTo(QualityLevel.MEDIUM)
        assertThat(QualityLevel.fromName("high")).isEqualTo(QualityLevel.HIGH)
        assertThat(QualityLevel.fromName("LOW")).isEqualTo(QualityLevel.LOW)
        assertThat(QualityLevel.fromName("PERFORMANCE")).isEqualTo(QualityLevel.MEDIUM)
    }

    @Test
    fun highHasMoreSamplesThanLow() {
        assertThat(QualityLevel.HIGH.smoothingSamples).isGreaterThan(QualityLevel.LOW.smoothingSamples)
        assertThat(QualityLevel.HIGH.analysisHz).isGreaterThan(QualityLevel.LOW.analysisHz)
    }

    @Test
    fun performanceIsAutomaticOnly() {
        assertThat(QualityLevel.PERFORMANCE.userSelectable).isFalse()
        assertThat(QualityLevel.PERFORMANCE.geometryEnabled).isFalse()
        assertThat(QualityLevel.PERFORMANCE.detailRestorationEnabled).isFalse()
        assertThat(QualityLevel.PERFORMANCE.featureEnhancementEnabled).isFalse()
        assertThat(QualityLevel.userChoices).doesNotContain(QualityLevel.PERFORMANCE)
    }

    @Test
    fun maskResolutionMonotonic() {
        assertThat(QualityLevel.PERFORMANCE.maskResolution).isLessThan(QualityLevel.LOW.maskResolution)
        assertThat(QualityLevel.LOW.maskResolution).isLessThan(QualityLevel.MEDIUM.maskResolution)
        assertThat(QualityLevel.MEDIUM.maskResolution).isAtMost(QualityLevel.HIGH.maskResolution)
    }

}
