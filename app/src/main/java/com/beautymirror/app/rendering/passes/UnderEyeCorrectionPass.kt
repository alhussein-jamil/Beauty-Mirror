package com.beautymirror.app.rendering.passes

import android.opengl.GLES30
import com.beautymirror.app.rendering.GlFramebuffer
import com.beautymirror.app.rendering.GlMesh
import com.beautymirror.app.rendering.GlProgram
import com.beautymirror.app.rendering.GlTexture
import com.beautymirror.app.rendering.RenderPass

class UnderEyeCorrectionPass(vertexSrc: String, fragmentSrc: String) : RenderPass {
    override val name = "under_eye"
    override var enabled = true
    private val program = GlProgram(vertexSrc, fragmentSrc)
    private val uInput = program.uniformLocation("uInput")
    private val uUnderEyeMask = program.uniformLocation("uUnderEyeMask")
    private val uLeftCheekUv = program.uniformLocation("uLeftCheekUv")
    private val uRightCheekUv = program.uniformLocation("uRightCheekUv")
    private val uLeftCheekRef = program.uniformLocation("uLeftCheekRef")
    private val uRightCheekRef = program.uniformLocation("uRightCheekRef")
    private val uStrength = program.uniformLocation("uStrength")
    private val uMaxLift = program.uniformLocation("uMaxLift")
    private val uColorCorrection = program.uniformLocation("uColorCorrection")
    private val uSmoothing = program.uniformLocation("uSmoothing")
    private val uLeftVisibility = program.uniformLocation("uLeftVisibility")
    private val uRightVisibility = program.uniformLocation("uRightVisibility")
    private val uPoseWeight = program.uniformLocation("uPoseWeight")
    private val uEnabled = program.uniformLocation("uEnabled")
    private val uTexelSize = program.uniformLocation("uTexelSize")

    var strength = 0f
    var maxLift = 0.18f
    var colorCorrection = 0.35f
    var smoothing = 0f
    var leftVisibility = 1f
    var rightVisibility = 1f
    var poseWeight = 1f
    var underEyeMask: GlTexture? = null
    var leftCheek = floatArrayOf(0.78f, 0.64f, 0.58f)
    var rightCheek = floatArrayOf(0.78f, 0.64f, 0.58f)
    var leftCheekUv = floatArrayOf(0.35f, 0.55f)
    var rightCheekUv = floatArrayOf(0.65f, 0.55f)
    private var texelW = 1f
    private var texelH = 1f

    override fun resize(width: Int, height: Int) {
        texelW = 1f / width.coerceAtLeast(1)
        texelH = 1f / height.coerceAtLeast(1)
    }

    override fun render(input: GlTexture, outputFbo: GlFramebuffer, mesh: GlMesh) {
        outputFbo.bind()
        program.use()
        val mask = underEyeMask
        val on = enabled &&
            (strength > 0.001f || smoothing > 0.001f || colorCorrection > 0.001f || maxLift > 0.001f) &&
            mask != null
        input.bind(0)
        (mask ?: input).bind(1)
        GLES30.glUniform1i(uInput, 0)
        GLES30.glUniform1i(uUnderEyeMask, 1)
        GLES30.glUniform2f(uLeftCheekUv, leftCheekUv[0], leftCheekUv[1])
        GLES30.glUniform2f(uRightCheekUv, rightCheekUv[0], rightCheekUv[1])
        GLES30.glUniform3f(uLeftCheekRef, leftCheek[0], leftCheek[1], leftCheek[2])
        GLES30.glUniform3f(uRightCheekRef, rightCheek[0], rightCheek[1], rightCheek[2])
        GLES30.glUniform1f(uStrength, strength)
        GLES30.glUniform1f(uMaxLift, maxLift)
        GLES30.glUniform1f(uColorCorrection, colorCorrection)
        GLES30.glUniform1f(uSmoothing, smoothing)
        GLES30.glUniform1f(uLeftVisibility, leftVisibility)
        GLES30.glUniform1f(uRightVisibility, rightVisibility)
        GLES30.glUniform1f(uPoseWeight, poseWeight)
        GLES30.glUniform1f(uEnabled, if (on) 1f else 0f)
        GLES30.glUniform2f(uTexelSize, texelW, texelH)
        mesh.draw()
        outputFbo.unbind()
    }

    override fun release() = program.delete()
}
