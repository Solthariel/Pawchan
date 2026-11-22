package com.solthariel.pawchan.navigation.chrome

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal data class AppBottomItemState(
    val label: String,
    val icon: Int,
    val selected: Boolean? = null,
    val badge: Int = 0,
)

/** Navigation tabs and the contextual writing action share 48dp targets, not selection pills. */
@Composable
internal fun RowScope.AppBottomItem(
    state: AppBottomItemState,
    onClick: (() -> Unit)?,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val enabled = onClick != null
    val color =
        if (state.selected == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val interaction =
        if (state.selected != null) {
            Modifier.selectable(
                state.selected,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Tab,
            ) { onClick?.invoke() }
        } else {
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
            ) { onClick?.invoke() }
        }
    Box(
        Modifier.weight(1f).fillMaxSize().then(interaction).semantics {
            contentDescription = if (state.badge > 0) "${state.label}, ответов: ${state.badge}" else state.label
        },
        contentAlignment = Alignment.Center,
    ) {
        // Keep the whole cell clickable; only its centered 48dp circle draws the press indication.
        Box(
            Modifier.size(48.dp).clip(CircleShape).indication(interactionSource, LocalIndication.current),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(state.icon),
                null,
                tint = color.copy(alpha = if (enabled && state.selected != false) 1f else DISABLED_ALPHA),
                modifier = Modifier.size(24.dp),
            )
        }
        if (state.badge > 0) AppBottomBadge(state.badge)
    }
}

private const val DISABLED_ALPHA = 0.55f
