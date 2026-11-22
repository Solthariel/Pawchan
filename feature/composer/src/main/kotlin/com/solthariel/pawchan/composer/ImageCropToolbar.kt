package com.solthariel.pawchan.composer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.drafts.DraftCrop
import com.solthariel.pawchan.domain.drafts.cropToAspect

@Composable
internal fun ImageCropToolbar(
    state: ImageEditorState,
    sourceWidth: Int,
    sourceHeight: Int,
    preset: ImageCropPreset,
    enabled: Boolean = true,
    onPresetChange: (ImageCropPreset) -> Unit,
) {
    val controlsEnabled = enabled && !state.inGesture && sourceWidth > 0 && sourceHeight > 0
    Column {
        var ratiosOpen by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { ratiosOpen = true }, enabled = controlsEnabled) {
                Text("Пропорции: ${preset.label} ▾")
            }
            DropdownMenu(expanded = ratiosOpen, onDismissRequest = { ratiosOpen = false }) {
                ImageCropPreset.entries.forEach { option ->
                    DropdownMenuItem(text = { Text(option.label) }, onClick = {
                        ratiosOpen = false
                        option.aspect(state.edit, sourceWidth, sourceHeight)?.let { ratio ->
                            state.commit(state.edit.cropToAspect(sourceWidth, sourceHeight, ratio))
                        }
                        onPresetChange(option)
                    })
                }
            }
        }
        val actionsScroll = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        Row(actionsScroll, horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = {
                state.commit(state.edit.rotateLeft())
                onPresetChange(preset.rotated())
            }, enabled = controlsEnabled) { Text("Повернуть") }
            val horizontal = Modifier.semantics { contentDescription = "Отразить по горизонтали" }
            val vertical = Modifier.semantics { contentDescription = "Отразить по вертикали" }
            val flipHorizontal = { state.commit(state.edit.flipHorizontal()) }
            TextButton(onClick = flipHorizontal, modifier = horizontal, enabled = controlsEnabled) { Text("↔") }
            val flipVertical = { state.commit(state.edit.flipVertical()) }
            TextButton(onClick = flipVertical, modifier = vertical, enabled = controlsEnabled) {
                Text("↕")
            }
            TextButton(onClick = {
                state.commit(state.edit.copy(crop = DraftCrop(), quarterTurns = 0, mirrored = false))
                onPresetChange(ImageCropPreset.FREE)
            }, enabled = controlsEnabled) { Text("Сбросить") }
        }
    }
}
