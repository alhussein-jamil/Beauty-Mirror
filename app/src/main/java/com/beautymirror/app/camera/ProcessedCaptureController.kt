package com.beautymirror.app.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import com.beautymirror.app.BuildConfig
import com.beautymirror.app.rendering.BeautyRenderer
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class ProcessedCaptureController(
    private val context: Context,
    private val renderer: BeautyRenderer,
    private val glHandler: Handler,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    sealed class CaptureResult {
        data class Success(val uriString: String) : CaptureResult()
        data class Failure(val message: String, val cause: Throwable? = null) : CaptureResult()
    }

    private val captureMutex = Mutex()
    private var pixelScratch: IntArray? = null

    suspend fun capture(width: Int, height: Int): CaptureResult = captureMutex.withLock {
        try {
            val rgba = captureOnGlThread(width, height)
                ?: return@withLock CaptureResult.Failure("GL capture returned no pixels")
            withContext(ioDispatcher) {
                val bitmap = rgbaToBitmap(rgba, width, height, flipY = true)
                try {
                    val uri = saveBitmap(bitmap)
                    CaptureResult.Success(uri.toString())
                } finally {
                    bitmap.recycle()
                }
            }
        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) Log.e(TAG, "capture failed", t)
            CaptureResult.Failure(t.message ?: "capture failed", t)
        }
    }

    private suspend fun captureOnGlThread(width: Int, height: Int): ByteBuffer? =
        suspendCancellableCoroutine { continuation ->
            val posted = glHandler.post {
                val result = runCatching {
                    // Read-only duplicate keeps independent position/limit while sharing the reusable
                    // native storage. captureMutex prevents a second capture from overwriting it.
                    renderer.captureRgba(width, height)?.asReadOnlyBuffer()
                }
                if (!continuation.isActive) return@post
                result.fold(
                    onSuccess = { continuation.resume(it) },
                    onFailure = { continuation.resume(null) },
                )
            }
            if (!posted && continuation.isActive) continuation.resume(null)
        }

    private fun rgbaToBitmap(buf: ByteBuffer, width: Int, height: Int, flipY: Boolean): Bitmap {
        require(width > 0 && height > 0)
        val needBytes = width.toLong() * height.toLong() * 4L
        require(needBytes <= Int.MAX_VALUE && buf.capacity() >= needBytes.toInt()) {
            "Invalid RGBA buffer for ${width}x$height"
        }
        val pixelsNeeded = width * height
        val pixels = pixelScratch?.takeIf { it.size >= pixelsNeeded }
            ?: IntArray(pixelsNeeded).also { pixelScratch = it }

        for (y in 0 until height) {
            val srcY = if (flipY) height - 1 - y else y
            for (x in 0 until width) {
                val offset = (srcY * width + x) * 4
                val r = buf.get(offset).toInt() and 0xff
                val g = buf.get(offset + 1).toInt() and 0xff
                val b = buf.get(offset + 2).toInt() and 0xff
                val a = buf.get(offset + 3).toInt() and 0xff
                pixels[y * width + x] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun saveBitmap(bitmap: Bitmap): android.net.Uri {
        val name = "BeautyMirror_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
            put(MediaStore.Images.Media.ORIENTATION, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BeautyMirror")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("MediaStore insert failed")
        try {
            resolver.openOutputStream(uri)?.use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) {
                    "JPEG compression failed"
                }
            } ?: error("MediaStore output stream unavailable")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            return uri
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            throw t
        }
    }

    companion object {
        private const val TAG = "ProcessedCapture"
    }
}
