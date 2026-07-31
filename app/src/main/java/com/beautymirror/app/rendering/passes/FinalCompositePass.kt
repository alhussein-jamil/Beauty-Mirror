package com.beautymirror.app.rendering.passes

import android.opengl.GLES30
import com.beautymirror.app.rendering.GlFramebuffer
import com.beautymirror.app.rendering.GlMesh
import com.beautymirror.app.rendering.GlProgram
import com.beautymirror.app.rendering.GlTexture
import com.beautymirror.app.rendering.RenderPass

class FinalCompositePass(vertexSrc: String, fragmentSrc: String) : RenderPass {
    override val name = "final_composite"
    override var enabled = true
    private val program = GlProgram(vertexSrc, fragmentSrc)
    private val uProcessed = program.uniformLocation("uProcessed")
    private val uOriginal = program.uniformLocation("uOriginal")
    private val uBeforeAfter = program.uniformLocation("uBeforeAfter")
    private val uDim = program.uniformLocation("uDim")

    var original: GlTexture? = null
    var beforeAfter = 0f
    var dimAmount = 0f

    override fun resize(width: Int, height: Int) = Unit

    /** Renders directly to currently bound draw framebuffer (screen). */
    fun renderToScreen(processed: GlTexture, mesh: GlMesh, width: Int, height: Int) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, width, height)
        draw(processed, mesh)
    }

    override fun render(input: GlTexture, outputFbo: GlFramebuffer, mesh: GlMesh) {
        outputFbo.bind()
        draw(input, mesh)
        outputFbo.unbind()
    }

    private fun draw(processed: GlTexture, mesh: GlMesh) {
        program.use()
        processed.bind(0)
        (original ?: processed).bind(1)
        GLES30.glUniform1i(uProcessed, 0)
        GLES30.glUniform1i(uOriginal, 1)
        GLES30.glUniform1f(uBeforeAfter, beforeAfter)
        GLES30.glUniform1f(uDim, dimAmount)
        mesh.draw()
    }

    override fun release() = program.delete()
}
