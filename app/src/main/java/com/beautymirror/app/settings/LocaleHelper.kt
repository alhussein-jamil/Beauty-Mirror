package com.beautymirror.app.settings

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

object LanguagePreferences {
    private const val PREFS = "bm_locale"
    private const val KEY = "lang"

    private fun store(context: Context) =
        (context.applicationContext ?: context).getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(context: Context): AppLanguage {
        val tag = store(context).getString(KEY, AppLanguage.ENGLISH.tag)
        return AppLanguage.fromTag(tag)
    }

    fun set(context: Context, language: AppLanguage) {
        store(context).edit().putString(KEY, language.tag).apply()
    }
}

object LocaleHelper {
    fun wrap(context: Context, language: AppLanguage = LanguagePreferences.get(context)): Context {
        val locale = language.locale
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }
}
