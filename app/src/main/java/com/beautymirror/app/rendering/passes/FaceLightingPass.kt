package com.beautymirror.app.rendering.passes

import android.opengl.GLES30
import com.beautymirror.app.rendering.GlFramebuffer
import com.beautymirror.app.rendering.GlMesh
import com.beautymirror.app.rendering.GlProgram
import com.beautymirror.app.rendering.GlTexture
import com.beautymirror.app.rendering.RenderPass
import kotlin.math.abs

class FaceLightingPass(vertexSrc: String, fragmentSrc: String) : RenderPass {
    override val name = "face_lighting"
    override var enabled = true
    private val program = GlProgram(vertexSrc, fragmentSrc)
    private val uInput = program.uniformLocation("uInput")
    private val uFaceMask = program.uniformLocation("uFaceMask")
    private val uFaceExposure = program.uniformLocation("uFaceExposure")
    private val uShadowLift = program.uniformLocation("uShadowLift")
    private val uHighlightProtection = program.uniformLocation("uHighlightProtection")
    private val uLocalContrast = program.uniformLocation("uLocalContrast")
    private val uFaceLuma = program.uniformLocation("uFaceLuma")
    private val uShineControl = program.uniformLocation("uShineControl")
    private val uSkinGlow = program.uniformLocation("uSkinGlow")
    private val uEnabled = program.uniformLocation("uEnabled")

    var faceExposure = 0f
    var shadowLift = 0f
    var highlightProtection = 0.35f
    var localContrast = 0f
    var faceLuma = 0.5f
    var shineControl = 0f
    var skinGlow = 0f
    var faceMask: GlTexture? = null

    override fun resize(width: Int, height: Int) = Unit

    override fun render(input: GlTexture, outputFbo: GlFramebuffer, mesh: GlMesh) {
        outputFbo.bind()
        program.use()
        val mask = faceMask
        val on = enabled && mask != null && (
            abs(faceExposure) > 0.001f ||
                shadowLift > 0.001f ||
                localContrast > 0.001f ||
                shineControl > 0.001f ||
                skinGlow > 0.001f
            )
        input.bind(0)
        (mask ?: input).bind(1)
        GLES30.glUniform1i(uInput, 0)
        GLES30.glUniform1i(uFaceMask, 1)
        GLES30.glUniform1f(uFaceExposure, faceExposure)
        GLES30.glUniform1f(uShadowLift, shadowLift)
        GLES30.glUniform1f(uHighlightProtection, highlightProtection)
        GLES30.glUniform1f(uLocalContrast, localContrast)
        GLES30.glUniform1f(uFaceLuma, faceLuma)
        GLES30.glUniform1f(uShineControl, shineControl)
        GLES30.glUniform1f(uSkinGlow, skinGlow)
        GLES30.glUniform1f(uEnabled, if (on) 1f else 0f)
        mesh.draw()
        outputFbo.unbind()
    }

    override fun release() = program.delete()
}
