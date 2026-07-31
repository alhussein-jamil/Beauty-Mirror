package com.beautymirror.app

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.beautymirror.app.settings.LocaleHelper
import com.beautymirror.app.ui.BeautyMirrorApp

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        configureRealtimePerformance()
        val app = application as BeautyMirrorApplication
        val exhibitionMode = intent?.getBooleanExtra(EXTRA_EXHIBITION_MODE, false) == true
        setContent {
            BeautyMirrorApp(
                settingsRepository = app.settingsRepository,
                launchExhibitionMode = exhibitionMode,
            )
        }
        publishGameState(isLoading = false)
    }

    override fun onResume() {
        super.onResume()
        publishGameState(isLoading = false)
    }

    private fun configureRealtimePerformance() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Prefer a 30 Hz display mode when the panel supports it (matches quality target).
        val attrs = window.attributes
        attrs.preferredRefreshRate = 30f
        window.attributes = attrs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            window.setSustainedPerformanceMode(true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setPreferMinimalPostProcessing(true)
        }
    }

    /**
     * Signals Android Game Manager (API 33+) that we are in active realtime gameplay so OEM
     * boosters / Game Dashboard performance mode can engage. Uses reflection because some
     * compile stubs omit [android.app.Activity.setGameState].
     */
    private fun publishGameState(isLoading: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        runCatching {
            val gameStateClass = Class.forName("android.app.GameState")
            // MODE_GAMEPLAY_INTERRUPTIBLE = 1
            val mode = if (isLoading) 0 else 1
            val state = gameStateClass
                .getConstructor(Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                .newInstance(isLoading, mode)
            javaClass.methods
                .first { it.name == "setGameState" && it.parameterTypes.size == 1 }
                .invoke(this, state)
        }
    }

    companion object {
        const val EXTRA_EXHIBITION_MODE = "com.beautymirror.app.EXHIBITION_MODE"
    }
}
