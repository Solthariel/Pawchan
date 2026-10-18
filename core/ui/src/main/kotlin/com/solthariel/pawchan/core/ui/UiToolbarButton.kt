package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    Box(
        modifier =
            Modifier
                .size(APP_TOOLBAR_BUTTON_SIZE)
                .clip(CircleShape)
                .clickable(enabled = enabled, onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        UiIcon(
            kind = kind,
            color =
                when {
                    active -> MaterialTheme.colorScheme.primary
                    enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.outline
                },
            modifier = Modifier.size(24.dp).rotate(rotationDegrees),
        )
    }
}

val APP_TOOLBAR_BUTTON_SIZE = 36.dp
