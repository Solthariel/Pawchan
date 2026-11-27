package com.solthariel.pawchan.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.core.ui.R.string as L

/** Three quick presets and one editable percentage share the same four-cell row. */
@Composable
fun ImageQualityControl(
    quality: Int,
    enabled: Boolean,
    connected: Boolean = false,
    onChange: (Int) -> Unit,
) {
    val res = LocalResources.current

    var editing by rememberSaveable { mutableStateOf(false) }
    val presets = listOf(QUALITY_SMALL, QUALITY_BALANCED, QUALITY_HIGH)
    val presetIndex = presets.indexOf(quality)
    val manualQuality = if (presetIndex < 0) quality else QUALITY_MAX
    UiOptionRow(res.getString(L.ui_quality)) {
        val options = presets.map { "$it%" } + "$manualQuality%"
        val selected = if (presetIndex < 0) presets.size else presetIndex
        val select: (Int) -> Unit = { index ->
            if (index == presets.size) editing = true else onChange(presets[index])
        }
        if (connected) {
            UiConnectedChoices(options, selected, enabled, res.getString(L.ui_enter_jpeg_quality), select)
        } else {
            UiChoiceSegments(options, selected, enabled, res.getString(L.ui_enter_jpeg_quality), onSelect = select)
        }
    }
    if (editing) {
        NumberInputDialog(
            res.getString(L.ui_jpeg_quality_906d),
            manualQuality,
            1..QUALITY_MAX,
            { editing = false },
        ) {
            onChange(it)
            editing = false
        }
    }
}

private const val QUALITY_SMALL = 75
private const val QUALITY_BALANCED = 85
private const val QUALITY_HIGH = 95
private const val QUALITY_MAX = 100
