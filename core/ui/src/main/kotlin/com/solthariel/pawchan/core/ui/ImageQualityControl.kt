package com.solthariel.pawchan.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/** Three quick presets and one editable percentage share the same four-cell row. */
@Composable
fun ImageQualityControl(
    quality: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    val presets = listOf(QUALITY_SMALL, QUALITY_BALANCED, QUALITY_HIGH)
    val presetIndex = presets.indexOf(quality)
    val manualQuality = if (presetIndex < 0) quality else QUALITY_MAX
    UiOptionRow("Качество") {
        UiChoiceSegments(
            options = presets.map { "$it%" } + "$manualQuality%",
            selected = if (presetIndex < 0) presets.size else presetIndex,
            enabled = enabled,
            lastOptionDescription = "Ввести качество JPEG",
        ) { index ->
            if (index == presets.size) editing = true else onChange(presets[index])
        }
    }
    if (editing) {
        NumberInputDialog("Качество JPEG, %", manualQuality, 1..QUALITY_MAX, { editing = false }) {
            onChange(it)
            editing = false
        }
    }
}

private const val QUALITY_SMALL = 75
private const val QUALITY_BALANCED = 85
private const val QUALITY_HIGH = 95
private const val QUALITY_MAX = 100
