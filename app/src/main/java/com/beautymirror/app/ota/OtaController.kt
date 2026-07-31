package com.beautymirror.app.ota

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.beautymirror.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground poller (~5 min) matching Bokko OTA lifecycle behavior.
 */
class OtaController(
    context: Context,
    private val service: UpdateService = UpdateService(context.applicationContext),
) : DefaultLifecycleObserver {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pollJob: Job? = null

    val updateService: UpdateService get() = service

    fun start() {
        if (!BuildConfig.OTA_ENABLED) return
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        onStart(ProcessLifecycleOwner.get())
    }

    fun stop() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        pollJob?.cancel()
        pollJob = null
    }

    fun checkNow() {
        scope.launch {
            service.checkForUpdate(force = true)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            // Short delay so camera init wins the first seconds.
            delay(8_000)
            while (isActive) {
                if (OtaPreferences.isAutoUpdateEnabled(appContext)) {
                    service.checkForUpdate(force = false)
                }
                delay(POLL_MS)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        pollJob?.cancel()
        pollJob = null
    }

    companion object {
        private const val POLL_MS = 5 * 60_000L
    }
}
