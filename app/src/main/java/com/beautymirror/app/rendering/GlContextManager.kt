package com.beautymirror.app.rendering

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.view.Surface

/** Persistent GLES 3 EGL context for the beauty pipeline. All calls are GL-thread confined. */
class GlContextManager {
    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var context: EGLContext = EGL14.EGL_NO_CONTEXT
    private var config: EGLConfig? = null
    private var currentSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var boundWindow: Surface? = null
    private var pbufferBound: Boolean = false

    fun initialize() {
        check(display == EGL14.EGL_NO_DISPLAY) { "EGL already initialized" }
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(display != EGL14.EGL_NO_DISPLAY) { "eglGetDisplay failed: ${eglError()}" }

        val version = IntArray(2)
        check(EGL14.eglInitialize(display, version, 0, version, 1)) {
            "eglInitialize failed: ${eglError()}"
        }
        check(EGL14.eglBindAPI(EGL14.EGL_OPENGL_ES_API)) {
            "eglBindAPI failed: ${eglError()}"
        }

        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT or EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val num = IntArray(1)
        check(EGL14.eglChooseConfig(display, attribList, 0, configs, 0, 1, num, 0)) {
            "eglChooseConfig failed: ${eglError()}"
        }
        check(num[0] > 0 && configs[0] != null) { "No GLES 3 EGL config available" }
        config = configs[0]

        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
        context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
        check(context != EGL14.EGL_NO_CONTEXT) { "eglCreateContext failed: ${eglError()}" }
    }

    fun makeCurrent(surface: Surface) {
        check(surface.isValid) { "Output Surface is not valid" }
        ensureInitialized()
        if (boundWindow === surface && currentSurface != EGL14.EGL_NO_SURFACE && !pbufferBound) {
            if (EGL14.eglGetCurrentContext() != context) {
                check(EGL14.eglMakeCurrent(display, currentSurface, currentSurface, context)) {
                    "eglMakeCurrent rebind failed: ${eglError()}"
                }
            }
            return
        }

        releaseSurface()
        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        currentSurface = EGL14.eglCreateWindowSurface(display, config, surface, surfaceAttribs, 0)
        check(currentSurface != EGL14.EGL_NO_SURFACE) {
            "eglCreateWindowSurface failed: ${eglError()}"
        }
        check(EGL14.eglMakeCurrent(display, currentSurface, currentSurface, context)) {
            "eglMakeCurrent failed: ${eglError()}"
        }
        boundWindow = surface
        pbufferBound = false
    }

    fun makeCurrentPbuffer(width: Int = 1, height: Int = 1) {
        ensureInitialized()
        if (pbufferBound && currentSurface != EGL14.EGL_NO_SURFACE) {
            if (EGL14.eglGetCurrentContext() != context) {
                check(EGL14.eglMakeCurrent(display, currentSurface, currentSurface, context)) {
                    "pbuffer rebind failed: ${eglError()}"
                }
            }
            return
        }

        releaseSurface()
        val attribs = intArrayOf(
            EGL14.EGL_WIDTH, width.coerceAtLeast(1),
            EGL14.EGL_HEIGHT, height.coerceAtLeast(1),
            EGL14.EGL_NONE,
        )
        currentSurface = EGL14.eglCreatePbufferSurface(display, config, attribs, 0)
        check(currentSurface != EGL14.EGL_NO_SURFACE) {
            "eglCreatePbufferSurface failed: ${eglError()}"
        }
        check(EGL14.eglMakeCurrent(display, currentSurface, currentSurface, context)) {
            "pbuffer makeCurrent failed: ${eglError()}"
        }
        boundWindow = null
        pbufferBound = true
    }

    fun swapBuffers() {
        check(currentSurface != EGL14.EGL_NO_SURFACE) { "No EGL surface bound" }
        check(EGL14.eglSwapBuffers(display, currentSurface)) {
            "eglSwapBuffers failed: ${eglError()}"
        }
    }

    fun setPresentationTime(ns: Long) {
        if (currentSurface != EGL14.EGL_NO_SURFACE) {
            EGLExt.eglPresentationTimeANDROID(display, currentSurface, ns)
        }
    }

    fun releaseSurface() {
        if (currentSurface != EGL14.EGL_NO_SURFACE) {
            // Unbind both context and surface. A surfaceless current context is optional EGL
            // functionality and fails on a meaningful subset of Android drivers.
            EGL14.eglMakeCurrent(
                display,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT,
            )
            EGL14.eglDestroySurface(display, currentSurface)
            currentSurface = EGL14.EGL_NO_SURFACE
        }
        boundWindow = null
        pbufferBound = false
    }

    fun release() {
        releaseSurface()
        if (context != EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroyContext(display, context)
            context = EGL14.EGL_NO_CONTEXT
        }
        if (display != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(display)
            display = EGL14.EGL_NO_DISPLAY
        }
        config = null
    }

    private fun ensureInitialized() {
        check(display != EGL14.EGL_NO_DISPLAY && context != EGL14.EGL_NO_CONTEXT) {
            "EGL is not initialized"
        }
    }

    private fun eglError(): String = "0x${Integer.toHexString(EGL14.eglGetError())}"
}
