package com.solthariel.pawchan.locale

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.R
import com.solthariel.pawchan.media.video.MediaLocaleHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LanguageDeliveryRuntimeTest {
    @Test fun legacyOverrideResolvesJapaneseAndEmptyChoiceReturnsTheSameBase() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val original =
            base.resources.configuration.locales
                .toLanguageTags()
        val japanese = legacyContext(base, "ja")
        assertEquals("インターフェース言語", japanese.getString(R.string.interface_language))
        assertEquals(
            original,
            base.resources.configuration.locales
                .toLanguageTags(),
        )
        assertSame(base, legacyContext(base, ""))
    }

    @Test fun mediaHostUsesAndroidOrReadsStoredLegacyChoice() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val host = target.applicationContext as MediaLocaleHost
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android already applied the chosen locale. Do not set or overwrite the real app's system language.
            assertSame(target, host.localizedMediaContext(target))
        } else {
            val prefs = target.getSharedPreferences("locale-delivery-test", Context.MODE_PRIVATE)
            val base =
                object : ContextWrapper(target) {
                    override fun getSharedPreferences(
                        name: String?,
                        mode: Int,
                    ): SharedPreferences = prefs
                }
            try {
                prefs.edit { putString("selected", "de") }
                assertEquals("de", PawchanLocales.selected(base))
                assertEquals("Oberflächensprache", host.localizedMediaContext(base).getString(R.string.interface_language))
                prefs.edit { remove("selected") }
                assertSame(base, host.localizedMediaContext(base))
            } finally {
                prefs.edit { clear() }
            }
        }
    }
}
