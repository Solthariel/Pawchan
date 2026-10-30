package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun UiToolbarButton(
    kind: UiIconKind,
    description: String,
    rotationDegrees: Float = 0f,
    enabled: Boolean = true,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(APP_TOOLBAR_BUTTON_SIZE).semantics { contentDescription = description },
        colors =
            IconButtonDefaults.iconButtonColors(
                contentColor =
                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    ) {
        UiIcon(
            kind = kind,
            color = LocalContentColor.current,
            modifier = Modifier.size(24.dp).rotate(rotationDegrees),
        )
    }
}

/** Standard Material action target; panel slots use the same geometry. */
val APP_TOOLBAR_BUTTON_SIZE = 48.dp
