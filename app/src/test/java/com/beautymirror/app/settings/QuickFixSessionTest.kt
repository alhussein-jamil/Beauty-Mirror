package com.beautymirror.app.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QuickFixSessionTest {
    @Test
    fun undoingOneFixKeepsSiblingFixes() {
        val session = QuickFixSession()
        val base = BeautySettings.natural().copy(
            underEyeStrength = 0.10f,
            smoothingStrength = 0.10f,
        )
        val afterEyes = session.toggle("eyes", base) { s ->
            s.copy(underEyeStrength = 0.80f)
        }
        assertThat(afterEyes.underEyeStrength).isEqualTo(0.80f)

        val afterBoth = session.toggle("skin", afterEyes) { s ->
            s.copy(smoothingStrength = 0.70f)
        }
        assertThat(afterBoth.underEyeStrength).isEqualTo(0.80f)
        assertThat(afterBoth.smoothingStrength).isEqualTo(0.70f)

        val afterUndoEyes = session.toggle("eyes", afterBoth) { s ->
            s.copy(underEyeStrength = 0.80f)
        }
        assertThat(afterUndoEyes.underEyeStrength).isEqualTo(0.10f)
        assertThat(afterUndoEyes.smoothingStrength).isEqualTo(0.70f)
        assertThat(session.activeIds).containsExactly("skin")
    }

    @Test
    fun clearingRestoresBaseline() {
        val session = QuickFixSession()
        val base = BeautySettings.natural().copy(eyeClarity = 0.05f)
        session.toggle("eyes", base) { it.copy(eyeClarity = 0.55f) }
        val restored = session.clear()
        assertThat(restored?.eyeClarity).isEqualTo(0.05f)
        assertThat(session.activeIds).isEmpty()
    }
}
