package com.beautymirror.app.rendering.passes

import android.opengl.GLES30
import com.beautymirror.app.rendering.GlFramebuffer
import com.beautymirror.app.rendering.GlMesh
import com.beautymirror.app.rendering.GlProgram
import com.beautymirror.app.rendering.GlTexture
import com.beautymirror.app.rendering.RenderPass

/**
 * Procedural sky-pond installation pass.
 *
 * Idle is an animated sky-blue pond. With a visitor, the camera merges into the water; settled
 * water/camera amounts, ripple region count and wave detail are curator-controlled.
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
    private val uCameraBlend = program.uniformLocation("uCameraBlend")
    private val uDeformation = program.uniformLocation("uDeformation")
    private val uSwirl = program.uniformLocation("uSwirl")
    private val uSettledWater = program.uniformLocation("uSettledWater")
    private val uSettledCamera = program.uniformLocation("uSettledCamera")
    private val uRippleRegions = program.uniformLocation("uRippleRegions")
    private val uRippleSpeed = program.uniformLocation("uRippleSpeed")
    private val uWaveDetail = program.uniformLocation("uWaveDetail")
    private val uSpecular = program.uniformLocation("uSpecular")
    private val uSkyBlue = program.uniformLocation("uSkyBlue")
    private val uSunlight = program.uniformLocation("uSunlight")
    private val uWaterWarmth = program.uniformLocation("uWaterWarmth")
    private val uSaturation = program.uniformLocation("uSaturation")
    private val uFoam = program.uniformLocation("uFoam")
    private val uClouds = program.uniformLocation("uClouds")
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
    var intensity: Float = 0.92f
    var motion: Float = 0.55f
    var darkness: Float = 0.08f
    var faceClarity: Float = 0.90f
    var cameraBlend: Float = 0.58f
    var deformation: Float = 0.40f
    var swirl: Float = 0.70f
    var settledWater: Float = 0.42f
    var settledCamera: Float = 0.72f
    var rippleRegions: Float = 0.40f
    var rippleSpeed: Float = 0.55f
    var waveDetail: Float = 0.55f
    var specular: Float = 0.50f
    var skyBlue: Float = 0.78f
    var sunlight: Float = 0.70f
    var waterWarmth: Float = 0.22f
    var saturation: Float = 0.78f
    var foam: Float = 0.55f
    var clouds: Float = 0.55f
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
        GLES30.glUniform1f(uCameraBlend, cameraBlend.coerceIn(0f, 1f))
        GLES30.glUniform1f(uDeformation, deformation.coerceIn(0f, 1f))
        GLES30.glUniform1f(uSwirl, swirl.coerceIn(0f, 1f))
        GLES30.glUniform1f(uSettledWater, settledWater.coerceIn(0f, 1f))
        GLES30.glUniform1f(uSettledCamera, settledCamera.coerceIn(0f, 1f))
        GLES30.glUniform1f(uRippleRegions, rippleRegions.coerceIn(0f, 1f))
        GLES30.glUniform1f(uRippleSpeed, rippleSpeed.coerceIn(0f, 1f))
        GLES30.glUniform1f(uWaveDetail, waveDetail.coerceIn(0f, 1f))
        GLES30.glUniform1f(uSpecular, specular.coerceIn(0f, 1f))
        GLES30.glUniform1f(uSkyBlue, skyBlue.coerceIn(0f, 1f))
        GLES30.glUniform1f(uSunlight, sunlight.coerceIn(0f, 1f))
        GLES30.glUniform1f(uWaterWarmth, waterWarmth.coerceIn(0f, 1f))
        GLES30.glUniform1f(uSaturation, saturation.coerceIn(0f, 1f))
        GLES30.glUniform1f(uFoam, foam.coerceIn(0f, 1f))
        GLES30.glUniform1f(uClouds, clouds.coerceIn(0f, 1f))
        GLES30.glUniform1f(uQuality, quality.coerceIn(0f, 1f))
        GLES30.glUniform1f(uEnabled, if (enabled && intensity > 0.001f) 1f else 0f)
        mesh.draw()
        outputFbo.unbind()
    }

    override fun release() = program.delete()
}
