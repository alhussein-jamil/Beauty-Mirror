package com.beautymirror.app.camera

import android.util.Log
import android.view.Surface
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.core.util.Consumer
import com.beautymirror.app.BuildConfig
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference

/**
 * Provides the OpenGL-backed camera input [Surface] to CameraX Preview.
 * The renderer receives the exact requested buffer resolution and transformation metadata.
 */
class PreviewSurfaceProvider(
    private val executor: Executor,
    private val surfaceProvider: (SurfaceRequest) -> Surface,
    private val onTransformationInfo: (
        request: SurfaceRequest,
        info: SurfaceRequest.TransformationInfo,
    ) -> Unit = { _, _ -> },
    private val onFailure: (Throwable) -> Unit = {},
) : Preview.SurfaceProvider {
    private val latestRequest = AtomicReference<SurfaceRequest?>(null)

    override fun onSurfaceRequested(request: SurfaceRequest) {
        val previous = latestRequest.getAndSet(request)
        if (previous != null && previous !== request) {
            runCatching { previous.willNotProvideSurface() }
        }
        request.addRequestCancellationListener(executor) {
            latestRequest.compareAndSet(request, null)
        }
        request.setTransformationInfoListener(
            executor,
            SurfaceRequest.TransformationInfoListener { info ->
                if (latestRequest.get() === request) {
                    onTransformationInfo(request, info)
                }
            },
        )
        try {
            if (latestRequest.get() !== request) {
                request.willNotProvideSurface()
                return
            }
            val surface = surfaceProvider(request)
            if (latestRequest.get() !== request) {
                request.willNotProvideSurface()
                return
            }
            request.provideSurface(
                surface,
                executor,
                Consumer { result ->
                    latestRequest.compareAndSet(request, null)
                    if (BuildConfig.DEBUG && result.resultCode != SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY) {
                        Log.w(TAG, "CameraX surface finished with code=${result.resultCode}")
                    }
                },
            )
        } catch (t: Throwable) {
            latestRequest.compareAndSet(request, null)
            runCatching { request.willNotProvideSurface() }
            onFailure(t)
        }
    }

    companion object {
        private const val TAG = "PreviewSurfaceProvider"
    }
}
