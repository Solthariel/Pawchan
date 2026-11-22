package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.solthariel.pawchan.domain.settings.AppFont
import com.solthariel.pawchan.domain.settings.FontEmphasis
import com.solthariel.pawchan.domain.settings.TextLeading
import com.solthariel.pawchan.domain.settings.TypographyPreferences

internal fun Preferences.readTypography(): TypographyPreferences =
    TypographyPreferences(
        font = AppFont.entries.firstOrNull { it.name == this[FONT] } ?: AppFont.SYSTEM,
        emphasis = FontEmphasis.entries.firstOrNull { it.name == this[EMPHASIS] } ?: FontEmphasis.NORMAL,
        leading = TextLeading.entries.firstOrNull { it.name == this[LEADING] } ?: TextLeading.STANDARD,
    )

internal fun MutablePreferences.writeTypography(value: TypographyPreferences) {
    this[FONT] = value.font.name
    this[EMPHASIS] = value.emphasis.name
    this[LEADING] = value.leading.name
}

private val FONT = stringPreferencesKey("typography_font")
private val EMPHASIS = stringPreferencesKey("typography_emphasis")
private val LEADING = stringPreferencesKey("typography_leading")
