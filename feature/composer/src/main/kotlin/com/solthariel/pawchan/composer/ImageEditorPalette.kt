package com.solthariel.pawchan.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.feature.composer.R.string as L

@Composable
internal fun ImageEditorPalette(
    selection: ImageToolSelection,
    enabled: Boolean,
    redaction: Boolean = false,
    onSelected: () -> Unit = {},
) {
    val res = LocalResources.current

    Column {
        IMAGE_EDITOR_COLORS.chunked(PALETTE_COLUMNS).forEachIndexed { row, colors ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                colors.forEachIndexed { column, color ->
                    val index = row * PALETTE_COLUMNS + column
                    val selected = color == if (redaction) selection.redactionColor else selection.color
                    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    val semantics =
                        Modifier.semantics {
                            contentDescription = res.getString(L.ui_color, index + 1)
                            this.selected = selected
                        }
                    Box(
                        semantics
                            .size(48.dp)
                            .clickable(enabled = enabled) {
                                if (redaction) selection.redactionColor = color else selection.color = color
                                onSelected()
                            }.padding(10.dp)
                            .border(if (selected) 3.dp else 1.dp, border, CircleShape)
                            .background(Color(color), CircleShape),
                    )
                }
            }
        }
    }
}

private const val PALETTE_COLUMNS = 4
