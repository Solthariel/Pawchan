package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
internal fun ImageBrushControls(
    selection: ImageToolSelection,
    enabled: Boolean,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ImageColorControl(selection, enabled && selection.drawing != ImageDrawingTool.ERASER)
        Slider(
            value = selection.width,
            onValueChange = { selection.width = it },
            valueRange = .005f..0.12f,
            enabled = enabled,
            modifier =
                Modifier.weight(1f).semantics {
                    contentDescription =
                        if (selection.drawing == ImageDrawingTool.ERASER) "Размер ластика" else "Толщина"
                },
        )
    }
}
