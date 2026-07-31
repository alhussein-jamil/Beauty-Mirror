package com.beautymirror.app.rendering.passes

import android.opengl.GLES30
import com.beautymirror.app.rendering.FaceEffectGeometry
import com.beautymirror.app.rendering.GlFramebuffer
import com.beautymirror.app.rendering.GlMesh
import com.beautymirror.app.rendering.GlProgram
import com.beautymirror.app.rendering.GlTexture
import com.beautymirror.app.rendering.RenderPass

class FeatureEnhancementPass(vertexSrc: String, fragmentSrc: String) : RenderPass {
    override val name = "feature_enhancement"
    override var enabled = true

    private val program = GlProgram(vertexSrc, fragmentSrc)
    private val uInput = program.uniformLocation("uInput")
    private val uEyeMask = program.uniformLocation("uEyeMask")
    private val uBrowMask = program.uniformLocation("uBrowMask")
    private val uLipMask = program.uniformLocation("uLipMask")
    private val uMouthMask = program.uniformLocation("uMouthMask")
    private val uFaceMask = program.uniformLocation("uFaceMask")
    private val uLeftCheek = program.uniformLocation("uLeftCheek")
    private val uRightCheek = program.uniformLocation("uRightCheek")
    private val uLeftJaw = program.uniformLocation("uLeftJaw")
    private val uRightJaw = program.uniformLocation("uRightJaw")
    private val uNoseCenter = program.uniformLocation("uNoseCenter")
    private val uMouthCenter = program.uniformLocation("uMouthCenter")
    private val uFaceWidth = program.uniformLocation("uFaceWidth")
    private val uFaceHeight = program.uniformLocation("uFaceHeight")
    private val uFaceAxisX = program.uniformLocation("uFaceAxisX")
    private val uFaceAxisY = program.uniformLocation("uFaceAxisY")
    private val uLeftVisibility = program.uniformLocation("uLeftVisibility")
    private val uRightVisibility = program.uniformLocation("uRightVisibility")
    private val uPoseWeight = program.uniformLocation("uPoseWeight")
    private val uEyeBrightening = program.uniformLocation("uEyeBrightening")
    private val uBrowDefinition = program.uniformLocation("uBrowDefinition")
    private val uTeethWhitening = program.uniformLocation("uTeethWhitening")
    private val uLipEnhancement = program.uniformLocation("uLipEnhancement")
    private val uLipTintStrength = program.uniformLocation("uLipTintStrength")
    private val uLipDefinition = program.uniformLocation("uLipDefinition")
    private val uLipGloss = program.uniformLocation("uLipGloss")
    private val uBlush = program.uniformLocation("uBlush")
    private val uContour = program.uniformLocation("uContour")
    private val uOpacity = program.uniformLocation("uOpacity")
    private val uEnabled = program.uniformLocation("uEnabled")

    var eyeMask: GlTexture? = null
    var browMask: GlTexture? = null
    var lipMask: GlTexture? = null
    var mouthMask: GlTexture? = null
    var faceMask: GlTexture? = null
    var geometry: FaceEffectGeometry? = null
    var eyeBrightening = 0f
    var browDefinition = 0f
    var teethWhitening = 0f
    var lipEnhancement = 0f
    var lipTintStrength = 0f
    var lipDefinition = 0f
    var lipGloss = 0f
    var blush = 0f
    var contour = 0f
    var opacity = 0f

    override fun resize(width: Int, height: Int) = Unit

    override fun render(input: GlTexture, outputFbo: GlFramebuffer, mesh: GlMesh) {
        outputFbo.bind()
        program.use()
        val g = geometry
        val on = enabled && g != null && eyeMask != null && browMask != null && lipMask != null &&
            mouthMask != null && faceMask != null && opacity > 0.001f && (
                eyeBrightening > 0.001f || browDefinition > 0.001f || teethWhitening > 0.001f ||
                    lipEnhancement > 0.001f || lipTintStrength > 0.001f ||
                    lipDefinition > 0.001f || lipGloss > 0.001f ||
                    blush > 0.001f || contour > 0.001f
                )
        input.bind(0)
        (eyeMask ?: input).bind(1)
        (browMask ?: input).bind(2)
        (lipMask ?: input).bind(3)
        (mouthMask ?: input).bind(4)
        (faceMask ?: input).bind(5)
        GLES30.glUniform1i(uInput, 0)
        GLES30.glUniform1i(uEyeMask, 1)
        GLES30.glUniform1i(uBrowMask, 2)
        GLES30.glUniform1i(uLipMask, 3)
        GLES30.glUniform1i(uMouthMask, 4)
        GLES30.glUniform1i(uFaceMask, 5)
        if (g != null) {
            GLES30.glUniform2f(uLeftCheek, g.leftCheek.x, g.leftCheek.y)
            GLES30.glUniform2f(uRightCheek, g.rightCheek.x, g.rightCheek.y)
            GLES30.glUniform2f(uLeftJaw, g.leftJaw.x, g.leftJaw.y)
            GLES30.glUniform2f(uRightJaw, g.rightJaw.x, g.rightJaw.y)
            GLES30.glUniform2f(uNoseCenter, g.noseCenter.x, g.noseCenter.y)
            GLES30.glUniform2f(uMouthCenter, g.mouthCenter.x, g.mouthCenter.y)
            GLES30.glUniform1f(uFaceWidth, g.faceWidth)
            GLES30.glUniform1f(uFaceHeight, g.faceHeight)
            GLES30.glUniform2f(uFaceAxisX, g.faceAxisX.x, g.faceAxisX.y)
            GLES30.glUniform2f(uFaceAxisY, g.faceAxisY.x, g.faceAxisY.y)
            GLES30.glUniform1f(uLeftVisibility, g.leftVisibility)
            GLES30.glUniform1f(uRightVisibility, g.rightVisibility)
            GLES30.glUniform1f(uPoseWeight, g.poseWeight)
        }
        GLES30.glUniform1f(uEyeBrightening, eyeBrightening)
        GLES30.glUniform1f(uBrowDefinition, browDefinition)
        GLES30.glUniform1f(uTeethWhitening, teethWhitening)
        GLES30.glUniform1f(uLipEnhancement, lipEnhancement)
        GLES30.glUniform1f(uLipTintStrength, lipTintStrength)
        GLES30.glUniform1f(uLipDefinition, lipDefinition)
        GLES30.glUniform1f(uLipGloss, lipGloss)
        GLES30.glUniform1f(uBlush, blush)
        GLES30.glUniform1f(uContour, contour)
        GLES30.glUniform1f(uOpacity, opacity)
        GLES30.glUniform1f(uEnabled, if (on) 1f else 0f)
        mesh.draw()
        outputFbo.unbind()
    }

    override fun release() = program.delete()
}
