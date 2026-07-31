package com.beautymirror.app.rendering

import android.opengl.GLES30

class GlFramebuffer {
    val id: Int
    var colorTexture: GlTexture? = null
        private set

    init {
        val ids = IntArray(1)
        GLES30.glGenFramebuffers(1, ids, 0)
        id = ids[0]
    }

    fun attach(texture: GlTexture) {
        colorTexture = texture
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, id)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT0,
            texture.target,
            texture.id,
            0,
        )
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            throw IllegalStateException("FBO incomplete: 0x${Integer.toHexString(status)}")
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    fun bind() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, id)
        colorTexture?.let { GLES30.glViewport(0, 0, it.width, it.height) }
    }

    fun unbind() = GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)

    private val deleteIds = IntArray(1)

    fun delete() {
        deleteIds[0] = id
        GLES30.glDeleteFramebuffers(1, deleteIds, 0)
    }
}
