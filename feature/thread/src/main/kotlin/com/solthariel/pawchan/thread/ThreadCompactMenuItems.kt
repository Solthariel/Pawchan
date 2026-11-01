package com.solthariel.pawchan.thread

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import com.solthariel.pawchan.core.ui.UiIconKind

/** Compact bars move, rather than remove, the two secondary navigation actions. */
@Composable
internal fun ThreadCompactMenuItems(
    tools: ThreadToolsState,
    content: ThreadUiState.Content?,
    actions: ThreadActions?,
) {
    ThreadMenuItem(
        if (content?.isFavorite == true) "Убрать из избранного" else "Добавить в избранное",
        enabled = content != null && actions != null,
        icon = if (content?.isFavorite == true) UiIconKind.BOOKMARK_FILLED else UiIconKind.BOOKMARK_ADD,
    ) {
        tools.closeMenu()
        actions?.onFavoriteToggle?.invoke()
    }
    ThreadMenuItem("Галерея", enabled = content != null, icon = UiIconKind.MEDIA_MULTIPLE) {
        tools.openDialog(ThreadDialog.GALLERY)
    }
    HorizontalDivider()
}
