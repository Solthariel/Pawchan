package com.solthariel.pawchan.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.AppAlertDialog
import com.solthariel.pawchan.domain.settings.ThemeColors
import com.solthariel.pawchan.domain.settings.ThemePalette
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.ui.generatedColorScheme
import com.solthariel.pawchan.ui.withPureBlackSurfaces
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun CustomColorDialog(
    state: SettingsUiState,
    onChange: (ThemeColors) -> Unit,
    onDismiss: () -> Unit,
) {
    val systemAccent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MaterialTheme.colorScheme.primary.toArgb() else null
    var hex by rememberSaveable { mutableStateOf(seedHex(initialCustomSeed(state.themeColors, systemAccent))) }
    val seed = parseSeedHex(hex)
    val dark =
        state.theme != ThemePreference.LIGHT &&
            (state.theme != ThemePreference.SYSTEM || androidx.compose.foundation.isSystemInDarkTheme())
    val black = dark && (state.theme == ThemePreference.AMOLED || state.themeColors.pureBlack)
    val request by rememberUpdatedState(seed?.let { ColorPreviewRequest(it, dark, black) })
    val preview by produceState<ColorPreviewFrame?>(null) {
        snapshotFlow { request }
            .colorPreviewFrames { next ->
                withContext(Dispatchers.Default) {
                    val colors = generatedColorScheme(next.seed, next.dark)
                    if (next.pureBlack) colors.withPureBlackSurfaces() else colors
                }
            }.collect { value = it }
    }
    val ready = request != null && preview?.request == request
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Свой цвет") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Выберите исходный цвет. Оттенки интерфейса будут подобраны автоматически.")
                ColorSeedInput(hex) { hex = it }
                ColorSchemePreview(preview?.colors ?: MaterialTheme.colorScheme)
            }
        },
        confirmButton = {
            TextButton(enabled = ready, onClick = {
                val current = parseSeedHex(hex)?.let { ColorPreviewRequest(it, dark, black) }
                if (current != null && preview?.request == current) {
                    onChange(state.themeColors.copy(palette = ThemePalette.CUSTOM, customSeed = current.seed))
                    onDismiss()
                }
            }) { Text("Применить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
