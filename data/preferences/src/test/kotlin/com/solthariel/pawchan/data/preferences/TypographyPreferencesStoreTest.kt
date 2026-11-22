package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.solthariel.pawchan.domain.settings.AppFont
import com.solthariel.pawchan.domain.settings.FontEmphasis
import com.solthariel.pawchan.domain.settings.TextLeading
import com.solthariel.pawchan.domain.settings.TypographyPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class TypographyPreferencesStoreTest {
    @Test
    fun oldAndUnknownValuesUseCompatibleDefaults() {
        assertEquals(TypographyPreferences(), emptyPreferences().readTypography())
        val stored = mutablePreferencesOf(stringPreferencesKey("typography_font") to "FUTURE")
        assertEquals(TypographyPreferences(), stored.readTypography())
    }

    @Test
    fun eachFontAndStyleRoundTrips() {
        for (font in AppFont.entries) {
            for (emphasis in FontEmphasis.entries) {
                for (leading in TextLeading.entries) {
                    val value = TypographyPreferences(font, emphasis, leading)
                    val stored = mutablePreferencesOf()
                    stored.writeTypography(value)
                    assertEquals(value, stored.readTypography())
                }
            }
        }
    }

    @Test
    fun portableBackupPreservesFontPreferences() {
        val chosen = TypographyPreferences(AppFont.ROBOTO_MONO, FontEmphasis.STRONG, TextLeading.RELAXED)
        val stored = mutablePreferencesOf()
        stored.writeTypography(chosen)
        val backup = exportPortablePreferences(stored)
        val restored = mutablePreferencesOf()
        copyPortableTo(restored, validatePortablePreferences(backup))
        assertEquals(chosen, restored.readTypography())
    }
}
