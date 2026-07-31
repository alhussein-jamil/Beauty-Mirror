package com.beautymirror.app.rendering

import android.opengl.GLES11Ext
import android.opengl.GLES30

class GlTexture(
    val target: Int = GLES30.GL_TEXTURE_2D,
    width: Int = 0,
    height: Int = 0,
    internalFormat: Int = GLES30.GL_RGBA8,
) {
    val id: Int
    var width: Int = width
        private set
    var height: Int = height
        private set

    init {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        id = ids[0]
        GLES30.glBindTexture(target, id)
        GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        if (target == GLES30.GL_TEXTURE_2D && width > 0 && height > 0) {
            allocate(width, height, internalFormat)
        }
        GLES30.glBindTexture(target, 0)
    }

    private var allocatedFormat = GLES30.GL_RGBA8

    fun allocate(w: Int, h: Int, internalFormat: Int = GLES30.GL_RGBA8) {
        if (w == width && h == height && internalFormat == allocatedFormat && width > 0) return
        width = w
        height = h
        allocatedFormat = internalFormat
        GLES30.glBindTexture(target, id)
        GLES30.glTexImage2D(target, 0, internalFormat, w, h, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
        GLES30.glBindTexture(target, 0)
    }

    fun bind(unit: Int) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(target, id)
    }

    private val deleteIds = IntArray(1)

    fun delete() {
        deleteIds[0] = id
        GLES30.glDeleteTextures(1, deleteIds, 0)
    }

    companion object {
        fun createOes(): GlTexture = GlTexture(target = GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
    }
}
