package com.beautymirror.app.rendering.passes

import android.opengl.GLES30
import com.beautymirror.app.rendering.GlFramebuffer
import com.beautymirror.app.rendering.GlMesh
import com.beautymirror.app.rendering.GlProgram
import com.beautymirror.app.rendering.GlTexture
import com.beautymirror.app.rendering.RenderPass

class DetailRestorationPass(vertexSrc: String, fragmentSrc: String) : RenderPass {
    override val name = "detail_restoration"
    override var enabled = true
    private val program = GlProgram(vertexSrc, fragmentSrc)
    private val uInput = program.uniformLocation("uInput")
    private val uOriginal = program.uniformLocation("uOriginal")
    private val uEyeMask = program.uniformLocation("uEyeMask")
    private val uDetailMask = program.uniformLocation("uDetailMask")
    private val uEyeClarity = program.uniformLocation("uEyeClarity")
    private val uDetailPreservation = program.uniformLocation("uDetailPreservation")
    private val uEyeSparkle = program.uniformLocation("uEyeSparkle")
    private val uTexelSize = program.uniformLocation("uTexelSize")
    private val uEnabled = program.uniformLocation("uEnabled")

    var eyeClarity = 0f
    var detailPreservation = 0.75f
    var eyeSparkle = 0f
    var original: GlTexture? = null
    var eyeMask: GlTexture? = null
    var detailMask: GlTexture? = null
    private var texelW = 1f
    private var texelH = 1f

    override fun resize(width: Int, height: Int) {
        texelW = 1f / width.coerceAtLeast(1)
        texelH = 1f / height.coerceAtLeast(1)
    }

    override fun render(input: GlTexture, outputFbo: GlFramebuffer, mesh: GlMesh) {
        outputFbo.bind()
        program.use()
        val on = enabled && (
            eyeClarity > 0.001f ||
                eyeSparkle > 0.001f ||
                detailPreservation > 0.001f
            ) && (eyeMask != null || detailMask != null)
        input.bind(0)
        (original ?: input).bind(1)
        (eyeMask ?: input).bind(2)
        (detailMask ?: input).bind(3)
        GLES30.glUniform1i(uInput, 0)
        GLES30.glUniform1i(uOriginal, 1)
        GLES30.glUniform1i(uEyeMask, 2)
        GLES30.glUniform1i(uDetailMask, 3)
        GLES30.glUniform1f(uEyeClarity, eyeClarity)
        GLES30.glUniform1f(uDetailPreservation, detailPreservation)
        GLES30.glUniform1f(uEyeSparkle, eyeSparkle)
        GLES30.glUniform2f(uTexelSize, texelW, texelH)
        GLES30.glUniform1f(uEnabled, if (on) 1f else 0f)
        mesh.draw()
        outputFbo.unbind()
    }

    override fun release() = program.delete()
}
