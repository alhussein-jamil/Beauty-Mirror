package com.beautymirror.app.ota

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReleaseMetaTest {
    @Test
    fun parsesBokkoShape() {
        val meta = ReleaseMeta.parse(
            """
            {
              "version": "3.2.0-debug",
              "buildNumber": 11,
              "sha256": "abc123",
              "size": 12345,
              "apkAssetName": "beauty-mirror-debug.apk"
            }
            """.trimIndent(),
        )
        assertThat(meta).isNotNull()
        assertThat(meta!!.buildNumber).isEqualTo(11)
        assertThat(meta.apkAssetName).isEqualTo("beauty-mirror-debug.apk")
        assertThat(meta.sha256).isEqualTo("abc123")
    }

    @Test
    fun rejectsIncomplete() {
        assertThat(ReleaseMeta.parse("""{"version":"1.0"}""")).isNull()
    }

    @Test
    fun comparesBuildNumbers() {
        assertThat(ReleaseMeta.isNewer(11, 10)).isTrue()
        assertThat(ReleaseMeta.isNewer(10, 10)).isFalse()
        assertThat(ReleaseMeta.isNewer(9, 10)).isFalse()
    }
}
