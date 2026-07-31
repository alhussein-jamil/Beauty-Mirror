package com.beautymirror.app.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LanguagePreferencesTest {
    @Test
    fun defaultsToEnglishAndPersistsFrench() {
        val context = RuntimeEnvironment.getApplication()
        assertThat(LanguagePreferences.get(context)).isEqualTo(AppLanguage.ENGLISH)
        LanguagePreferences.set(context, AppLanguage.FRENCH)
        assertThat(LanguagePreferences.get(context)).isEqualTo(AppLanguage.FRENCH)
        assertThat(AppLanguage.fromTag("fr")).isEqualTo(AppLanguage.FRENCH)
        assertThat(AppLanguage.fromTag("nope")).isEqualTo(AppLanguage.ENGLISH)
    }
}
