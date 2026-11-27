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
    val dark =
        when (preference) {
            ThemePreference.DARK -> true
            ThemePreference.LIGHT -> false
            ThemePreference.SYSTEM -> isSystemInDarkTheme()
            else -> error("Unsupported fixture theme")
        }
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}
