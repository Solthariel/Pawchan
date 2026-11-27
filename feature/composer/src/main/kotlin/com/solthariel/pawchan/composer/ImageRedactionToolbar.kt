package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.uiChoiceGroup
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool
import com.solthariel.pawchan.feature.composer.R
import com.solthariel.pawchan.feature.composer.R.string as L

@Composable
internal fun ImageRedactionToolbar(
    selection: ImageToolSelection,
    enabled: Boolean,
) {
    val res = LocalResources.current

    Column {
        ImageRedactionBrushControls(selection, enabled)
        Row(Modifier.fillMaxWidth()) {
            Row(Modifier.weight(1f).uiChoiceGroup().selectableGroup()) {
                DraftRedactionMode.entries.forEach { mode ->
                    val fill = mode == DraftRedactionMode.FILL
                    EditorChoiceButton(
                        EditorChoice(
                            if (fill) {
                                res.getString(
                                    L.ui_upload,
                                )
                            } else {
                                res.getString(L.ui_pixels)
                            },
                            if (fill) R.drawable.ic_tool_fill else R.drawable.ic_tool_pixels,
                            selected = selection.redactionMode == mode,
                        ),
                        enabled,
                        Modifier.weight(1f),
                    ) { selection.redactionMode = mode }
                }
            }
            Row(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
                    .uiChoiceGroup()
                    .selectableGroup(),
            ) {
                DraftRedactionTool.entries.forEach { tool ->
                    val rectangle = tool == DraftRedactionTool.RECTANGLE
                    EditorChoiceButton(
                        EditorChoice(
                            if (rectangle) {
                                res.getString(
                                    L.ui_with_a_frame,
                                )
                            } else {
                                res.getString(L.ui_with_brush)
                            },
                            if (rectangle) R.drawable.ic_tool_rectangle else R.drawable.ic_tool_brush,
                            selected = selection.redactionTool == tool,
                        ),
                        enabled,
                        Modifier.weight(1f),
                    ) { selection.redactionTool = tool }
                }
            }
        }
    }
}

@Composable
private fun ImageRedactionBrushControls(
    selection: ImageToolSelection,
    enabled: Boolean,
) {
    val res = LocalResources.current

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ImageColorControl(
            selection,
            enabled && selection.redactionMode == DraftRedactionMode.FILL,
            redaction = true,
        )
        Slider(
            value = selection.width,
            onValueChange = { selection.width = it },
            valueRange = .005f..0.12f,
            enabled = enabled && selection.redactionTool == DraftRedactionTool.BRUSH,
            modifier =
                Modifier.weight(1f).semantics {
                    contentDescription =
                        res.getString(L.ui_redaction_brush_size)
                },
        )
    }
}
