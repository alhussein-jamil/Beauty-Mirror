package com.beautymirror.app.camera

sealed class CameraState {
    data object Idle : CameraState()
    data object Starting : CameraState()
    data object Live : CameraState()
    data class Error(val message: String, val cause: Throwable? = null) : CameraState()
}
