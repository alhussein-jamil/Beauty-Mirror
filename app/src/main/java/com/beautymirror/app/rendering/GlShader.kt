package com.beautymirror.app.rendering

import android.opengl.GLES30
import android.util.Log
import com.beautymirror.app.BuildConfig

class GlShader(type: Int, source: String) {
    val id: Int = GLES30.glCreateShader(type)

    init {
        GLES30.glShaderSource(id, source)
        GLES30.glCompileShader(id)
        val status = IntArray(1)
        GLES30.glGetShaderiv(id, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(id)
            GLES30.glDeleteShader(id)
            throw IllegalStateException("Shader compile failed: $log")
        }
    }

    fun delete() {
        GLES30.glDeleteShader(id)
    }

    companion object {
        fun checkError(label: String) {
            if (!BuildConfig.DEBUG) return
            var err = GLES30.glGetError()
            while (err != GLES30.GL_NO_ERROR) {
                Log.e("GL", "$label: 0x${Integer.toHexString(err)}")
                err = GLES30.glGetError()
            }
        }
    }
}
