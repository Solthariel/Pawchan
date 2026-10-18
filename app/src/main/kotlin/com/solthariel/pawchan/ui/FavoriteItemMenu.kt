package com.solthariel.pawchan.ui

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppMenuDialog

@Composable
internal fun FavoriteItemMenu(
    onAction: (FavoriteMenuAction) -> Unit,
    onClose: () -> Unit,
    removeLabel: String = FavoriteMenuAction.REMOVE.label,
) {
    AppMenuDialog(onClose) {
        FavoriteMenuAction.entries.forEach { action ->
            val label = if (action == FavoriteMenuAction.REMOVE) removeLabel else action.label
            ActionMenuItem(label, { onAction(action) })
        }
    }
}
