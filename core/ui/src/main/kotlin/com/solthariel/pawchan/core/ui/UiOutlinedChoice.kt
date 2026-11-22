package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** A fixed surface and shape; selection changes only its outline and content color. */
@Composable
fun UiOutlinedChoice(
    selected: Boolean?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val tone = uiOptionColors(selected == true, enabled)
    val border =
        when (selected) {
            null -> Modifier.border(BorderStroke(1.dp, tone.foreground), shape)
            true -> Modifier.border(uiSelectionBorder(true, enabled), shape)
            false -> Modifier
        }
    val interaction =
        if (selected == null) {
            Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        } else {
            Modifier.selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
        }
    Box(
        modifier
            .clip(shape)
            .background(tone.background)
            .then(border)
            .then(interaction),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides tone.foreground) { content() }
    }
}
