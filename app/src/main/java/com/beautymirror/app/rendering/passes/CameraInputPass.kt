package com.beautymirror.app.rendering.passes

import android.opengl.GLES11Ext
import android.opengl.GLES30
import com.beautymirror.app.rendering.AspectCrop
import com.beautymirror.app.rendering.GlFramebuffer
import com.beautymirror.app.rendering.GlMesh
import com.beautymirror.app.rendering.GlProgram
import com.beautymirror.app.rendering.GlTexture
import com.beautymirror.app.rendering.RenderPass

/**
 * Converts the CameraX external OES texture into the graph's RGBA texture.
 *
 * CameraX's SurfaceTexture matrix handles producer rotation/crop. [cropScaleX]/[cropScaleY]
 * apply the final center-crop needed to fill the actual display aspect without stretching.
 */
class CameraInputPass(
    vertexSrc: String,
    fragmentSrc: String,
) : RenderPass {
    override val name: String = "camera_input"
    override var enabled: Boolean = true

    private val program = GlProgram(vertexSrc, fragmentSrc)
    private val uTexture = program.uniformLocation("uTexture")
    private val uTexMatrix = program.uniformLocation("uTexMatrix")
    private val uCropScale = program.uniformLocation("uCropScale")
    private val uMirrorX = program.uniformLocation("uMirrorX")
    private val texMatrix = FloatArray(16).also {
        android.opengl.Matrix.setIdentityM(it, 0)
    }

    private var inputWidth = 1
    private var inputHeight = 1
    private var outputWidth = 1
    private var outputHeight = 1
    private var inputRotationDegrees = 0
    private var cropScaleX = 1f
    private var cropScaleY = 1f
    var mirrorX: Boolean = false

    fun setTexMatrix(matrix: FloatArray) {
        require(matrix.size >= 16)
        System.arraycopy(matrix, 0, texMatrix, 0, 16)
    }

    fun setInputSize(width: Int, height: Int) {
        setInputTransform(width, height, inputRotationDegrees)
    }

    /** Rotation is used for aspect calculation only; SurfaceTexture supplies the pixel matrix. */
    fun setInputTransform(width: Int, height: Int, rotationDegrees: Int) {
        inputWidth = width.coerceAtLeast(1)
        inputHeight = height.coerceAtLeast(1)
        inputRotationDegrees = ((rotationDegrees % 360) + 360) % 360
        updateCropScale()
    }

    override fun resize(width: Int, height: Int) {
        outputWidth = width.coerceAtLeast(1)
        outputHeight = height.coerceAtLeast(1)
        updateCropScale()
    }

    private fun updateCropScale() {
        val crop = AspectCrop.centerCrop(
            sourceWidth = inputWidth,
            sourceHeight = inputHeight,
            destinationWidth = outputWidth,
            destinationHeight = outputHeight,
            rotationDegrees = inputRotationDegrees,
        )
        cropScaleX = crop.scaleX
        cropScaleY = crop.scaleY
    }

    override fun render(input: GlTexture, outputFbo: GlFramebuffer, mesh: GlMesh) {
        outputFbo.bind()
        program.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(input.target, input.id)
        GLES30.glUniform1i(uTexture, 0)
        GLES30.glUniformMatrix4fv(uTexMatrix, 1, false, texMatrix, 0)
        GLES30.glUniform2f(uCropScale, cropScaleX, cropScaleY)
        GLES30.glUniform1f(uMirrorX, if (mirrorX) 1f else 0f)
        mesh.draw()
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        outputFbo.unbind()
    }

    override fun release() = program.delete()
}
