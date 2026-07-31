package com.beautymirror.app.masks

import android.opengl.GLES30
import com.beautymirror.app.rendering.GlFramebuffer
import com.beautymirror.app.rendering.GlMesh
import com.beautymirror.app.rendering.GlProgram
import com.beautymirror.app.rendering.GlTexture

/**
 * Softens mask edges with a cheap separable blur (H then V).
 */
class MaskBlurPass(vertexSrc: String, fragmentSrc: String) {
    private val program = GlProgram(vertexSrc, fragmentSrc)
    private val uMask = program.uniformLocation("uMask")
    private val uTexelSize = program.uniformLocation("uTexelSize")
    private val uRadius = program.uniformLocation("uRadius")
    private val temp = GlTexture()
    private val tempFbo = GlFramebuffer()
    private var width = 0
    private var height = 0

    fun resize(w: Int, h: Int) {
        if (w == width && h == height) return
        width = w
        height = h
        temp.allocate(w, h)
        tempFbo.attach(temp)
    }

    fun blur(sourceAndDest: GlTexture, destFbo: GlFramebuffer, mesh: GlMesh, radius: Float) {
        if (width <= 0 || height <= 0) return
        val tw = 1f / width
        val th = 1f / height
        // Horizontal into temp
        tempFbo.bind()
        program.use()
        sourceAndDest.bind(0)
        GLES30.glUniform1i(uMask, 0)
        GLES30.glUniform2f(uTexelSize, tw, 0f)
        GLES30.glUniform1f(uRadius, radius)
        mesh.draw()
        tempFbo.unbind()
        // Vertical back into dest
        destFbo.bind()
        temp.bind(0)
        GLES30.glUniform2f(uTexelSize, 0f, th)
        GLES30.glUniform1f(uRadius, radius)
        mesh.draw()
        destFbo.unbind()
    }

    fun release() {
        program.delete()
        temp.delete()
        tempFbo.delete()
    }
}
