package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.solthariel.pawchan.core.ui.uiChoiceGroup
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun ImageDrawingToolbar(
    selection: ImageToolSelection,
    enabled: Boolean,
) {
    Column {
        ImageBrushControls(selection, enabled)
        Row(Modifier.fillMaxWidth().uiChoiceGroup().selectableGroup()) {
            ImageDrawingTool.entries.forEach { tool ->
                EditorChoiceButton(
                    EditorChoice(tool.label, drawingToolIcon(tool), selected = selection.drawing == tool),
                    enabled,
                    Modifier.weight(1f),
                ) { selection.drawing = tool }
            }
        }
    }
}

private fun drawingToolIcon(tool: ImageDrawingTool): Int =
    when (tool) {
        ImageDrawingTool.PEN -> R.drawable.ic_tool_pen
        ImageDrawingTool.MARKER -> R.drawable.ic_tool_marker
        ImageDrawingTool.ARROW -> R.drawable.ic_tool_arrow
        ImageDrawingTool.RECTANGLE -> R.drawable.ic_tool_rectangle
        ImageDrawingTool.OVAL -> R.drawable.ic_tool_oval
        ImageDrawingTool.ERASER -> R.drawable.ic_tool_eraser
    }
