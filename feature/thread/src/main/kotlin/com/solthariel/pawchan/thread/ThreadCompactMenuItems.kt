package com.solthariel.pawchan.thread

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.feature.thread.R.string as L

/** Compact bars move, rather than remove, the two secondary navigation actions. */
@Composable
internal fun ThreadCompactMenuItems(
    tools: ThreadToolsState,
    content: ThreadUiState.Content?,
    actions: ThreadActions?,
) {
    val res = LocalResources.current

    ThreadMenuItem(
        if (content?.isFavorite ==
            true
        ) {
            res.getString(L.ui_remove_from_favorites)
        } else {
            res.getString(L.ui_add_to_favorites)
        },
        enabled = content != null && actions != null,
        icon = if (content?.isFavorite == true) UiIconKind.BOOKMARK_FILLED else UiIconKind.BOOKMARK_ADD,
    ) {
        tools.closeMenu()
        actions?.onFavoriteToggle?.invoke()
    }
    ThreadMenuItem(res.getString(L.ui_gallery), enabled = content != null, icon = UiIconKind.MEDIA_MULTIPLE) {
        tools.openDialog(ThreadDialog.GALLERY)
    }
    HorizontalDivider()
}
