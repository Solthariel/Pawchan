package com.solthariel.pawchan.settings

import android.os.Build
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.core.ui.AppAlertDialog
import com.solthariel.pawchan.core.ui.UiForwardIndicator
import com.solthariel.pawchan.core.ui.modelLabel
import com.solthariel.pawchan.domain.settings.ThemeColors
import com.solthariel.pawchan.domain.settings.ThemePalette
import com.solthariel.pawchan.domain.settings.currentPalette
import com.solthariel.pawchan.R.string as L

@Composable
internal fun ThemePaletteRow(
    state: SettingsUiState,
    onChange: (ThemeColors) -> Unit,
) {
    val res = LocalResources.current

    var open by rememberSaveable { mutableStateOf(false) }
    val palette =
        if (state.themeColors.palette == ThemePalette.SYSTEM && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            ThemePalette.LAVENDER
        } else {
            state.themeColors.palette.currentPalette()
        }
    val summary =
        if (palette == ThemePalette.CUSTOM) {
            res.getString(L.ui_own_color, seedHex(state.themeColors.customSeed))
        } else {
            modelLabel(palette.paletteLabel())
        }
    SettingsRow(res.getString(L.ui_color_palette), summary, onClick = { open = true }) { UiForwardIndicator() }
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
    val res = LocalResources.current

    var custom by rememberSaveable { mutableStateOf(false) }
    if (custom) {
        CustomColorDialog(state, onChange, onDismiss)
    } else {
        AppAlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(res.getString(L.ui_color_palette)) },
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
            dismissButton = { TextButton(onClick = onDismiss) { Text(res.getString(L.ui_cancel)) } },
        )
    }
}
