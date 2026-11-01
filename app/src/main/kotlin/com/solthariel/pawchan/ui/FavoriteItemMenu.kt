package com.solthariel.pawchan.ui

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppMenuDialog
import com.solthariel.pawchan.core.ui.UiIconKind

@Composable
internal fun FavoriteItemMenu(
    onAction: (FavoriteMenuAction) -> Unit,
    onClose: () -> Unit,
    removeLabel: String = FavoriteMenuAction.REMOVE.label,
) {
    AppMenuDialog(onClose) {
        FavoriteMenuAction.entries.forEach { action ->
            val label = if (action == FavoriteMenuAction.REMOVE) removeLabel else action.label
            val icon =
                when (action) {
                    FavoriteMenuAction.OPEN_THREAD -> UiIconKind.FORUM
                    FavoriteMenuAction.COPY_LINK -> UiIconKind.LINK
                    FavoriteMenuAction.REMOVE -> UiIconKind.BOOKMARK_FILLED
                }
            ActionMenuItem(label, { onAction(action) }, icon = icon)
        }
    }
}
