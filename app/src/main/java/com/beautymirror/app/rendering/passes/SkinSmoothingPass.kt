package com.beautymirror.app.rendering.passes

import android.opengl.GLES30
import com.beautymirror.app.rendering.GlFramebuffer
import com.beautymirror.app.rendering.GlMesh
import com.beautymirror.app.rendering.GlProgram
import com.beautymirror.app.rendering.GlTexture
import com.beautymirror.app.rendering.RenderPass

class SkinSmoothingPass(vertexSrc: String, fragmentSrc: String) : RenderPass {
    override val name = "skin_smoothing"
    override var enabled = true
    private val program = GlProgram(vertexSrc, fragmentSrc)
    private val uInput = program.uniformLocation("uInput")
    private val uSkinMask = program.uniformLocation("uSkinMask")
    private val uDetailMask = program.uniformLocation("uDetailMask")
    private val uStrength = program.uniformLocation("uStrength")
    private val uRadius = program.uniformLocation("uRadius")
    private val uDetailRetention = program.uniformLocation("uDetailRetention")
    private val uComplexionEvenness = program.uniformLocation("uComplexionEvenness")
    private val uRednessCorrection = program.uniformLocation("uRednessCorrection")
    private val uBlemishControl = program.uniformLocation("uBlemishControl")
    private val uTexelSize = program.uniformLocation("uTexelSize")
    private val uEnabled = program.uniformLocation("uEnabled")
    private val uSampleCount = program.uniformLocation("uSampleCount")

    var strength = 0f
    var radius = 2f
    var detailRetention = 0.7f
    var complexionEvenness = 0f
    var rednessCorrection = 0f
    var blemishControl = 0f
    var sampleCount = 8
    var skinMask: GlTexture? = null
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
        val mask = skinMask
        val on = enabled && mask != null && (
            strength > 0.001f || complexionEvenness > 0.001f ||
                rednessCorrection > 0.001f || blemishControl > 0.001f
        )
        input.bind(0)
        (mask ?: input).bind(1)
        (detailMask ?: input).bind(2)
        GLES30.glUniform1i(uInput, 0)
        GLES30.glUniform1i(uSkinMask, 1)
        GLES30.glUniform1i(uDetailMask, 2)
        GLES30.glUniform1f(uStrength, strength)
        GLES30.glUniform1f(uRadius, radius)
        GLES30.glUniform1f(uDetailRetention, detailRetention)
        GLES30.glUniform1f(uComplexionEvenness, complexionEvenness)
        GLES30.glUniform1f(uRednessCorrection, rednessCorrection)
        GLES30.glUniform1f(uBlemishControl, blemishControl)
        GLES30.glUniform2f(uTexelSize, texelW, texelH)
        GLES30.glUniform1f(uEnabled, if (on) 1f else 0f)
        if (uSampleCount >= 0) {
            GLES30.glUniform1i(uSampleCount, sampleCount.coerceIn(4, 12))
        }
        mesh.draw()
        outputFbo.unbind()
    }

    override fun release() = program.delete()
}
