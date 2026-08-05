package com.beautymirror.app.masks

import android.opengl.GLES30
import com.beautymirror.app.rendering.GlFramebuffer
import com.beautymirror.app.rendering.GlMesh
import com.beautymirror.app.rendering.GlProgram
import com.beautymirror.app.rendering.GlTexture
import com.beautymirror.app.tracking.LandmarkPoint
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.hypot

/**
 * Renders landmark polygons into reusable mask FBOs with soft feathering via multi-pass alpha.
 * Hot path avoids per-frame heap allocations after warm-up.
 */
class MaskTextureRenderer(
    vertexSrc: String,
    fragmentSrc: String,
    blurVertexSrc: String,
    blurFragmentSrc: String,
) {
    private val program = GlProgram(vertexSrc, fragmentSrc)
    private val uColor = program.uniformLocation("uColor")
    private val vbo = IntArray(1)
    private var capacityFloats = 0
    private var vboBytes = 0
    private var scratchVerts: FloatArray = FloatArray(256)
    private var scratchBuf: FloatBuffer = ByteBuffer
        .allocateDirect(256 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    val faceMask = GlTexture()
    val skinMask = GlTexture()
    val underEyeMask = GlTexture() // R=left G=right
    val eyeMask = GlTexture()
    val browMask = GlTexture()
    val lipMask = GlTexture()
    val mouthMask = GlTexture()
    val detailMask = GlTexture()

    private val faceFbo = GlFramebuffer()
    private val skinFbo = GlFramebuffer()
    private val underEyeFbo = GlFramebuffer()
    private val eyeFbo = GlFramebuffer()
    private val browFbo = GlFramebuffer()
    private val lipFbo = GlFramebuffer()
    private val mouthFbo = GlFramebuffer()
    private val detailFbo = GlFramebuffer()

    private var width = 0
    private var height = 0
    private val exclusionGenerator = FeatureExclusionMaskGenerator()
    private val blur = MaskBlurPass(blurVertexSrc, blurFragmentSrc)

    init {
        GLES30.glGenBuffers(1, vbo, 0)
    }

    fun resize(w: Int, h: Int) {
        if (w == width && h == height) return
        width = w
        height = h
        faceMask.allocate(w, h)
        skinMask.allocate(w, h)
        underEyeMask.allocate(w, h)
        eyeMask.allocate(w, h)
        browMask.allocate(w, h)
        lipMask.allocate(w, h)
        mouthMask.allocate(w, h)
        detailMask.allocate(w, h)
        faceFbo.attach(faceMask)
        skinFbo.attach(skinMask)
        underEyeFbo.attach(underEyeMask)
        eyeFbo.attach(eyeMask)
        browFbo.attach(browMask)
        lipFbo.attach(lipMask)
        mouthFbo.attach(mouthMask)
        detailFbo.attach(detailMask)
        blur.resize(w, h)
    }

    fun render(
        polygons: MaskPolygons?,
        opacity: Float,
        faceWidthNorm: Float,
        mesh: GlMesh,
        secondBlur: Boolean = true,
        faceOnly: Boolean = false,
    ) {
        if (width <= 0 || height <= 0) return
        val feather = (faceWidthNorm * 0.04f).coerceIn(0.008f, 0.05f)
        clear(faceFbo)
        if (!faceOnly) {
            clear(skinFbo)
            clear(underEyeFbo)
            clear(eyeFbo)
            clear(browFbo)
            clear(lipFbo)
            clear(mouthFbo)
            clear(detailFbo)
        }
        if (polygons == null || opacity <= 0.01f) return

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        drawPolygon(faceFbo, polygons.faceOval, 1f, 1f, 1f, opacity, feather)
        if (faceOnly) {
            GLES30.glDisable(GLES30.GL_BLEND)
            val faceBlurRadius = (faceWidthNorm * 8f).coerceIn(1.2f, 3.5f)
            blur.blur(faceMask, faceFbo, mesh, faceBlurRadius * 0.85f)
            if (secondBlur && faceWidthNorm > 0.28f) {
                blur.blur(faceMask, faceFbo, mesh, faceBlurRadius * 0.46f)
            }
            return
        }

        drawPolygon(skinFbo, polygons.skin, 1f, 1f, 1f, opacity, feather)
        val exclusions = exclusionGenerator.exclusions(polygons)
        for (ex in exclusions) {
            drawPolygon(skinFbo, ex, 0f, 0f, 0f, 1f, feather * 0.6f)
        }
        drawPolygon(eyeFbo, polygons.leftEye, 1f, 1f, 1f, opacity, feather * 0.5f)
        drawPolygon(eyeFbo, polygons.rightEye, 1f, 1f, 1f, opacity, feather * 0.5f)
        // Iris boosts eye-clarity unsharp where iris/lash energy matters most.
        drawPolygon(eyeFbo, polygons.leftIris, 1f, 1f, 1f, opacity, feather * 0.35f)
        drawPolygon(eyeFbo, polygons.rightIris, 1f, 1f, 1f, opacity, feather * 0.35f)
        drawPolygon(browFbo, polygons.leftBrow, 1f, 1f, 1f, opacity, feather * 0.42f)
        drawPolygon(browFbo, polygons.rightBrow, 1f, 1f, 1f, opacity, feather * 0.42f)
        // Lip mask excludes the mouth interior; mouth mask is used for teeth candidate detection.
        drawPolygon(lipFbo, polygons.lips, 1f, 1f, 1f, opacity, feather * 0.50f)
        drawPolygon(lipFbo, polygons.mouthInterior, 0f, 0f, 0f, 1f, feather * 0.30f)
        drawPolygon(mouthFbo, polygons.mouthInterior, 1f, 1f, 1f, opacity, feather * 0.35f)
        drawPolygon(underEyeFbo, polygons.leftUnderEye, 1f, 0f, 0f, opacity, feather)
        drawPolygon(underEyeFbo, polygons.rightUnderEye, 0f, 1f, 0f, opacity, feather)
        drawPolygon(detailFbo, polygons.leftEye, 1f, 1f, 1f, opacity, feather)
        drawPolygon(detailFbo, polygons.rightEye, 1f, 1f, 1f, opacity, feather)
        drawPolygon(detailFbo, polygons.leftBrow, 1f, 1f, 1f, opacity, feather)
        drawPolygon(detailFbo, polygons.rightBrow, 1f, 1f, 1f, opacity, feather)
        drawPolygon(detailFbo, polygons.lips, 1f, 1f, 1f, opacity, feather)

        GLES30.glDisable(GLES30.GL_BLEND)

        // Soften hard polygon edges so effects do not show mask outlines.
        val blurRadius = (faceWidthNorm * 8f).coerceIn(1.2f, 3.5f)
        fun soft(tex: GlTexture, fbo: GlFramebuffer, scale: Float) {
            blur.blur(tex, fbo, mesh, blurRadius * scale)
            // Second pass for large faces — softer boundaries, fewer hard mask edges.
            if (secondBlur && faceWidthNorm > 0.28f) {
                blur.blur(tex, fbo, mesh, blurRadius * scale * 0.55f)
            }
        }
        soft(skinMask, skinFbo, 1f)
        soft(faceMask, faceFbo, 0.85f)
        soft(underEyeMask, underEyeFbo, 0.9f)
        soft(detailMask, detailFbo, 0.7f)
        soft(eyeMask, eyeFbo, 0.55f)
        soft(browMask, browFbo, 0.42f)
        soft(lipMask, lipFbo, 0.45f)
        soft(mouthMask, mouthFbo, 0.35f)
    }

    private fun clear(fbo: GlFramebuffer) {
        fbo.bind()
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        fbo.unbind()
    }

    private fun drawPolygon(
        fbo: GlFramebuffer,
        points: List<LandmarkPoint>,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
        feather: Float,
    ) {
        if (points.size < 3) return
        fbo.bind()
        program.use()
        drawFan(points, r, g, b, a * 0.55f, inflate = feather)
        drawFan(points, r, g, b, a, inflate = 0f)
        fbo.unbind()
    }

    private fun drawFan(points: List<LandmarkPoint>, r: Float, g: Float, b: Float, a: Float, inflate: Float) {
        var cx = 0f
        var cy = 0f
        for (p in points) {
            cx += p.x
            cy += p.y
        }
        val inv = 1f / points.size
        cx *= inv
        cy *= inv

        val floatCount = (points.size + 2) * 2
        ensureCapacity(floatCount)
        val verts = scratchVerts
        var idx = 0
        verts[idx++] = cx * 2f - 1f
        verts[idx++] = cy * 2f - 1f
        for (p in points) {
            var x = p.x
            var y = p.y
            if (inflate != 0f) {
                val dx = x - cx
                val dy = y - cy
                val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1e-4f)
                x += dx / len * inflate
                y += dy / len * inflate
            }
            verts[idx++] = x * 2f - 1f
            verts[idx++] = y * 2f - 1f
        }
        var x0 = points[0].x
        var y0 = points[0].y
        if (inflate != 0f) {
            val dx = x0 - cx
            val dy = y0 - cy
            val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1e-4f)
            x0 += dx / len * inflate
            y0 += dy / len * inflate
        }
        verts[idx++] = x0 * 2f - 1f
        verts[idx++] = y0 * 2f - 1f

        scratchBuf.clear()
        scratchBuf.put(verts, 0, floatCount)
        scratchBuf.flip()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
        val bytes = floatCount * 4
        if (bytes <= vboBytes) {
            GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, bytes, scratchBuf)
        } else {
            vboBytes = bytes
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, bytes, scratchBuf, GLES30.GL_DYNAMIC_DRAW)
        }
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 8, 0)
        GLES30.glUniform4f(uColor, r, g, b, a)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, floatCount / 2)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    private fun ensureCapacity(floats: Int) {
        if (floats <= capacityFloats) return
        capacityFloats = floats
        scratchVerts = FloatArray(floats)
        scratchBuf = ByteBuffer
            .allocateDirect(floats * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    }

    fun release() {
        program.delete()
        faceMask.delete(); skinMask.delete(); underEyeMask.delete(); eyeMask.delete()
        browMask.delete(); lipMask.delete(); mouthMask.delete(); detailMask.delete()
        faceFbo.delete(); skinFbo.delete(); underEyeFbo.delete(); eyeFbo.delete()
        browFbo.delete(); lipFbo.delete(); mouthFbo.delete(); detailFbo.delete()
        blur.release()
        GLES30.glDeleteBuffers(1, vbo, 0)
    }
}
