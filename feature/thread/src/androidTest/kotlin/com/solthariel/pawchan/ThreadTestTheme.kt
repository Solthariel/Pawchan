package com.solthariel.pawchan

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.solthariel.pawchan.domain.settings.ThemePreference

@Composable
internal fun ThreadTestTheme(
    preference: ThemePreference,
    content: @Composable () -> Unit,
) {
    require(preference == ThemePreference.DARK || preference == ThemePreference.SYSTEM)
    MaterialTheme(
        colorScheme = if (preference == ThemePreference.DARK || isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}
