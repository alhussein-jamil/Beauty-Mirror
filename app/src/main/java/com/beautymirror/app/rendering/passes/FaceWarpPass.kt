package com.beautymirror.app.rendering.passes

import android.opengl.GLES30
import com.beautymirror.app.rendering.FaceEffectGeometry
import com.beautymirror.app.rendering.GlFramebuffer
import com.beautymirror.app.rendering.GlMesh
import com.beautymirror.app.rendering.GlProgram
import com.beautymirror.app.rendering.GlTexture
import com.beautymirror.app.rendering.RenderPass

class FaceWarpPass(vertexSrc: String, fragmentSrc: String) : RenderPass {
    override val name = "face_warp"
    override var enabled = true

    private val program = GlProgram(vertexSrc, fragmentSrc)
    private val uInput = program.uniformLocation("uInput")
    private val uFaceMask = program.uniformLocation("uFaceMask")
    private val uLeftEye = program.uniformLocation("uLeftEye")
    private val uRightEye = program.uniformLocation("uRightEye")
    private val uLeftJaw = program.uniformLocation("uLeftJaw")
    private val uRightJaw = program.uniformLocation("uRightJaw")
    private val uFaceCenter = program.uniformLocation("uFaceCenter")
    private val uNoseCenter = program.uniformLocation("uNoseCenter")
    private val uNoseLeft = program.uniformLocation("uNoseLeft")
    private val uNoseRight = program.uniformLocation("uNoseRight")
    private val uFaceWidth = program.uniformLocation("uFaceWidth")
    private val uFaceHeight = program.uniformLocation("uFaceHeight")
    private val uLeftVisibility = program.uniformLocation("uLeftVisibility")
    private val uRightVisibility = program.uniformLocation("uRightVisibility")
    private val uPoseWeight = program.uniformLocation("uPoseWeight")
    private val uEyeEnlarge = program.uniformLocation("uEyeEnlarge")
    private val uFaceSlim = program.uniformLocation("uFaceSlim")
    private val uNoseRefine = program.uniformLocation("uNoseRefine")
    private val uOpacity = program.uniformLocation("uOpacity")
    private val uEnabled = program.uniformLocation("uEnabled")

    var faceMask: GlTexture? = null
    var geometry: FaceEffectGeometry? = null
    var eyeEnlarge = 0f
    var faceSlim = 0f
    var noseRefine = 0f
    var opacity = 0f

    override fun resize(width: Int, height: Int) = Unit

    override fun render(input: GlTexture, outputFbo: GlFramebuffer, mesh: GlMesh) {
        outputFbo.bind()
        program.use()
        val mask = faceMask
        val g = geometry
        val on = enabled && mask != null && g != null && opacity > 0.001f && (
            eyeEnlarge > 0.001f || faceSlim > 0.001f || noseRefine > 0.001f
        )
        input.bind(0)
        (mask ?: input).bind(1)
        GLES30.glUniform1i(uInput, 0)
        GLES30.glUniform1i(uFaceMask, 1)
        if (g != null) {
            GLES30.glUniform2f(uLeftEye, g.leftEye.x, g.leftEye.y)
            GLES30.glUniform2f(uRightEye, g.rightEye.x, g.rightEye.y)
            GLES30.glUniform2f(uLeftJaw, g.leftJaw.x, g.leftJaw.y)
            GLES30.glUniform2f(uRightJaw, g.rightJaw.x, g.rightJaw.y)
            GLES30.glUniform2f(uFaceCenter, g.faceCenter.x, g.faceCenter.y)
            GLES30.glUniform2f(uNoseCenter, g.noseCenter.x, g.noseCenter.y)
            GLES30.glUniform2f(uNoseLeft, g.noseLeft.x, g.noseLeft.y)
            GLES30.glUniform2f(uNoseRight, g.noseRight.x, g.noseRight.y)
            GLES30.glUniform1f(uFaceWidth, g.faceWidth)
            GLES30.glUniform1f(uFaceHeight, g.faceHeight)
            GLES30.glUniform1f(uLeftVisibility, g.leftVisibility)
            GLES30.glUniform1f(uRightVisibility, g.rightVisibility)
            GLES30.glUniform1f(uPoseWeight, g.poseWeight)
        }
        GLES30.glUniform1f(uEyeEnlarge, eyeEnlarge)
        GLES30.glUniform1f(uFaceSlim, faceSlim)
        GLES30.glUniform1f(uNoseRefine, noseRefine)
        GLES30.glUniform1f(uOpacity, opacity)
        GLES30.glUniform1f(uEnabled, if (on) 1f else 0f)
        mesh.draw()
        outputFbo.unbind()
    }

    override fun release() = program.delete()
}
