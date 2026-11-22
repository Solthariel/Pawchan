package com.solthariel.pawchan.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiOutlinedChoice

/** Parent data stays on the outer cell. Icons have the same 48dp targets and accessible labels. */
@Composable
internal fun EditorChoiceButton(
    choice: EditorChoice,
    enabled: Boolean,
    modifier: Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    onClick: () -> Unit,
) {
    if (choice.selected == null) {
        val colors = MaterialTheme.colorScheme
        Box(
            modifier
                .height(EDITOR_CONTROL_HEIGHT)
                .clip(shape)
                .background(colors.secondaryContainer.copy(alpha = if (enabled) 1f else .38f))
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = choice.label },
            contentAlignment = Alignment.Center,
        ) {
            ChoiceContent(choice, colors.onSecondaryContainer.copy(alpha = if (enabled) 1f else .38f))
        }
    } else {
        UiOutlinedChoice(
            selected = choice.selected,
            enabled = enabled,
            modifier = modifier.height(EDITOR_CONTROL_HEIGHT).semantics { contentDescription = choice.label },
            shape = shape,
            onClick = onClick,
        ) { ChoiceContent(choice, LocalContentColor.current) }
    }
}

@Composable
private fun ChoiceContent(
    choice: EditorChoice,
    tint: androidx.compose.ui.graphics.Color,
) {
    if (choice.icon != null) {
        Icon(painterResource(choice.icon), null, tint = tint, modifier = Modifier.size(24.dp))
    } else {
        Text(choice.caption.orEmpty(), style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
    }
}

internal val EDITOR_CONTROL_HEIGHT = 48.dp
