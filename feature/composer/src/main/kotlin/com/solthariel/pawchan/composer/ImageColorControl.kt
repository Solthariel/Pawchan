package com.solthariel.pawchan.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun ImageColorControl(
    selection: ImageToolSelection,
    enabled: Boolean,
    redaction: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    val description = if (redaction) "Цвет замазки" else "Выбрать цвет"
    Box {
        IconButton(
            onClick = { open = true },
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = description },
        ) {
            val color = if (redaction) selection.redactionColor else selection.color
            Box(
                Modifier
                    .size(24.dp)
                    .background(Color(color), CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ImageEditorPalette(selection, enabled, redaction) { open = false }
        }
    }
}
