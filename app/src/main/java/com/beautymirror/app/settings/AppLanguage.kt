package com.beautymirror.app.settings

import java.util.Locale

enum class AppLanguage(val tag: String) {
    ENGLISH("en"),
    FRENCH("fr"),
    ;

    val locale: Locale
        get() = Locale.forLanguageTag(tag)

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: ENGLISH
    }
}
