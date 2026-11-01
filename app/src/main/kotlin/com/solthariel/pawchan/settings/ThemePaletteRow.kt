package com.solthariel.pawchan.settings

import android.os.Build
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.core.ui.AppAlertDialog
import com.solthariel.pawchan.core.ui.UiForwardIndicator
import com.solthariel.pawchan.domain.settings.ThemeColors
import com.solthariel.pawchan.domain.settings.ThemePalette
import com.solthariel.pawchan.domain.settings.currentPalette

@Composable
internal fun ThemePaletteRow(
    state: SettingsUiState,
    onChange: (ThemeColors) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val palette =
        if (state.themeColors.palette == ThemePalette.SYSTEM && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            ThemePalette.LAVENDER
        } else {
            state.themeColors.palette.currentPalette()
        }
    val summary =
        if (palette == ThemePalette.CUSTOM) {
            "Свой цвет ${seedHex(state.themeColors.customSeed)}"
        } else {
            palette.paletteLabel()
        }
    SettingsRow("Цветовая палитра", summary, onClick = { open = true }) { UiForwardIndicator() }
    if (open) {
        ThemePaletteDialog(state, onChange, onDismiss = { open = false })
    }
}

@Composable
private fun ThemePaletteDialog(
    state: SettingsUiState,
    onChange: (ThemeColors) -> Unit,
    onDismiss: () -> Unit,
) {
    var custom by rememberSaveable { mutableStateOf(false) }
    if (custom) {
        CustomColorDialog(state, onChange, onDismiss)
    } else {
        AppAlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Цветовая палитра") },
            text = {
                PaletteChoiceList(state) { palette ->
                    if (palette == ThemePalette.CUSTOM) {
                        custom = true
                    } else {
                        onChange(state.themeColors.copy(palette = palette))
                        onDismiss()
                    }
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        )
    }
}
