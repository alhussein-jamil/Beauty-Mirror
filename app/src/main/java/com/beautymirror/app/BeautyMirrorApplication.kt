package com.beautymirror.app

import android.app.Application
import android.content.Context
import com.beautymirror.app.settings.LocaleHelper
import com.beautymirror.app.settings.SettingsRepository

class BeautyMirrorApplication : Application() {
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
    }
}
