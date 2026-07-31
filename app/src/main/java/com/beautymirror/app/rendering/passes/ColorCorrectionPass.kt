package com.beautymirror.app.rendering.passes

import android.opengl.GLES30
import com.beautymirror.app.rendering.GlFramebuffer
import com.beautymirror.app.rendering.GlMesh
import com.beautymirror.app.rendering.GlProgram
import com.beautymirror.app.rendering.GlTexture
import com.beautymirror.app.rendering.RenderPass
import kotlin.math.abs

class ColorCorrectionPass(vertexSrc: String, fragmentSrc: String) : RenderPass {
    override val name = "color_correction"
    override var enabled = true
    private val program = GlProgram(vertexSrc, fragmentSrc)
    private val uInput = program.uniformLocation("uInput")
    private val uFaceMask = program.uniformLocation("uFaceMask")
    private val uWarmth = program.uniformLocation("uWarmth")
    private val uLocalContrast = program.uniformLocation("uLocalContrast")
    private val uEnabled = program.uniformLocation("uEnabled")

    var warmth = 0f
    var localContrast = 0f
    var faceMask: GlTexture? = null

    override fun resize(width: Int, height: Int) = Unit

    override fun render(input: GlTexture, outputFbo: GlFramebuffer, mesh: GlMesh) {
        outputFbo.bind()
        program.use()
        val mask = faceMask
        val on = enabled && mask != null && (abs(warmth) > 0.001f || localContrast > 0.001f)
        input.bind(0)
        (mask ?: input).bind(1)
        GLES30.glUniform1i(uInput, 0)
        GLES30.glUniform1i(uFaceMask, 1)
        GLES30.glUniform1f(uWarmth, warmth)
        GLES30.glUniform1f(uLocalContrast, localContrast)
        GLES30.glUniform1f(uEnabled, if (on) 1f else 0f)
        mesh.draw()
        outputFbo.unbind()
    }

    override fun release() = program.delete()
}
