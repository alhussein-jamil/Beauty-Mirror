package com.beautymirror.app.rendering

import android.opengl.GLES30

class GlProgram(vertexSource: String, fragmentSource: String) {
    val id: Int = GLES30.glCreateProgram()
    private val vertex = GlShader(GLES30.GL_VERTEX_SHADER, vertexSource)
    private val fragment = GlShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)

    init {
        GLES30.glAttachShader(id, vertex.id)
        GLES30.glAttachShader(id, fragment.id)
        GLES30.glLinkProgram(id)
        val status = IntArray(1)
        GLES30.glGetProgramiv(id, GLES30.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(id)
            delete()
            throw IllegalStateException("Program link failed: $log")
        }
    }

    fun use() = GLES30.glUseProgram(id)

    fun uniformLocation(name: String): Int = GLES30.glGetUniformLocation(id, name)
    fun attribLocation(name: String): Int = GLES30.glGetAttribLocation(id, name)

    fun delete() {
        GLES30.glDeleteProgram(id)
        vertex.delete()
        fragment.delete()
    }
}
