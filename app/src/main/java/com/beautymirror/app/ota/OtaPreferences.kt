package com.beautymirror.app.ota

import android.content.Context

object OtaPreferences {
    private const val PREFS = "bm_ota"
    private const val KEY_AUTO = "auto_update_enabled"
    private const val KEY_COOLDOWN_UNTIL = "apply_cooldown_until_ms"

    fun isAutoUpdateEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO, true)

    fun setAutoUpdateEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO, enabled).apply()
    }

    fun cooldownActive(context: Context, nowMs: Long = System.currentTimeMillis()): Boolean =
        prefs(context).getLong(KEY_COOLDOWN_UNTIL, 0L) > nowMs

    fun markApplyFailed(context: Context, cooldownMs: Long = 30 * 60_000L) {
        prefs(context).edit()
            .putLong(KEY_COOLDOWN_UNTIL, System.currentTimeMillis() + cooldownMs)
            .apply()
    }

    fun clearCooldown(context: Context) {
        prefs(context).edit().remove(KEY_COOLDOWN_UNTIL).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
