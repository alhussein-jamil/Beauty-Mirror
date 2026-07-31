package com.beautymirror.app.rendering.passes

import android.opengl.GLES30
import com.beautymirror.app.rendering.GlFramebuffer
import com.beautymirror.app.rendering.GlMesh
import com.beautymirror.app.rendering.GlProgram
import com.beautymirror.app.rendering.GlTexture
import com.beautymirror.app.rendering.RenderPass

/**
 * One-pass dark puddle / Narcissus-pool treatment for exhibition use.
 *
 * Runs after beautification. Concentric expanding ripples bend UVs and light crests so motion
 * reads clearly; [faceClarity] at 1.0 keeps the face core still (no puddle warp). [quality]
 * only gates smear taps.
 */
class LakeReflectionPass(vertexSrc: String, fragmentSrc: String) : RenderPass {
    override val name: String = "lake_reflection"
    override var enabled: Boolean = true

    private val program = GlProgram(vertexSrc, fragmentSrc)
    private val uInput = program.uniformLocation("uInput")
    private val uTime = program.uniformLocation("uTime")
    private val uViewport = program.uniformLocation("uViewport")
    private val uFaceCenter = program.uniformLocation("uFaceCenter")
    private val uFaceSize = program.uniformLocation("uFaceSize")
    private val uFacePresence = program.uniformLocation("uFacePresence")
    private val uVisitorReveal = program.uniformLocation("uVisitorReveal")
    private val uIntensity = program.uniformLocation("uIntensity")
    private val uMotion = program.uniformLocation("uMotion")
    private val uDarkness = program.uniformLocation("uDarkness")
    private val uFaceClarity = program.uniformLocation("uFaceClarity")
    private val uQuality = program.uniformLocation("uQuality")
    private val uEnabled = program.uniformLocation("uEnabled")

    private var width = 1
    private var height = 1

    var timeSeconds: Float = 0f
    var faceCenterX: Float = 0.5f
    var faceCenterY: Float = 0.5f
    var faceWidth: Float = 0.38f
    var faceHeight: Float = 0.52f
    var facePresence: Float = 0f
    var visitorReveal: Float = 0f
    var intensity: Float = 0.78f
    var motion: Float = 0.55f
    var darkness: Float = 0.70f
    var faceClarity: Float = 1f
    var quality: Float = 1f

    override fun resize(width: Int, height: Int) {
        this.width = width.coerceAtLeast(1)
        this.height = height.coerceAtLeast(1)
    }

    override fun render(input: GlTexture, outputFbo: GlFramebuffer, mesh: GlMesh) {
        outputFbo.bind()
        program.use()
        input.bind(0)
        GLES30.glUniform1i(uInput, 0)
        GLES30.glUniform1f(uTime, timeSeconds)
        GLES30.glUniform2f(uViewport, width.toFloat(), height.toFloat())
        GLES30.glUniform2f(uFaceCenter, faceCenterX, faceCenterY)
        GLES30.glUniform2f(uFaceSize, faceWidth.coerceAtLeast(0.12f), faceHeight.coerceAtLeast(0.16f))
        GLES30.glUniform1f(uFacePresence, facePresence.coerceIn(0f, 1f))
        GLES30.glUniform1f(uVisitorReveal, visitorReveal.coerceIn(0f, 1f))
        GLES30.glUniform1f(uIntensity, intensity.coerceIn(0f, 1f))
        GLES30.glUniform1f(uMotion, motion.coerceIn(0f, 1f))
        GLES30.glUniform1f(uDarkness, darkness.coerceIn(0f, 1f))
        GLES30.glUniform1f(uFaceClarity, faceClarity.coerceIn(0f, 1f))
        GLES30.glUniform1f(uQuality, quality.coerceIn(0f, 1f))
        GLES30.glUniform1f(uEnabled, if (enabled && intensity > 0.001f) 1f else 0f)
        mesh.draw()
        outputFbo.unbind()
    }

    override fun release() = program.delete()
}
