package com.beautymirror.app.util

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

object ResourceUtils {
    fun loadAssetText(context: Context, path: String): String {
        context.assets.open(path).use { input ->
            return BufferedReader(InputStreamReader(input)).readText()
        }
    }

    fun loadAssetBytes(context: Context, path: String): ByteBuffer {
        context.assets.open(path).use { input ->
            val bytes = input.readBytes()
            return ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).also {
                it.put(bytes)
                it.rewind()
            }
        }
    }

    fun floatBuffer(data: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .also {
                it.put(data)
                it.position(0)
            }
}
