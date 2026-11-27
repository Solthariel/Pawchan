package com.solthariel.pawchan.threads

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppMenuDialog
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.R.string as L

/** Меню по долгому нажатию: убрать из секции, избранное, ссылка. */
@Composable
internal fun ThreadsRowMenu(
    target: ThreadsMenuTarget,
    favorites: Set<ThreadAddress>,
    actions: ThreadsScreenActions,
    onClose: () -> Unit,
) {
    val res = LocalResources.current

    val row = target.row
    val run: (() -> Unit) -> () -> Unit = { action ->
        {
            onClose()
            action()
        }
    }
    val favorite = row.address in favorites
    AppMenuDialog(onClose) {
        Text(
            row.title,
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        when (target.section) {
            ThreadsSection.PAGES -> {
                ActionMenuItem(
                    res.getString(L.ui_close),
                    run { actions.pages.close(row.address) },
                    icon = UiIconKind.CLOSE,
                )
            }

            ThreadsSection.FAVORITES -> {
                Unit
            }

            ThreadsSection.REPLIES -> {
                ActionMenuItem(
                    res.getString(L.ui_remove_from_replies),
                    run { actions.dismissReplies(row.address) },
                    icon = UiIconKind.DELETE,
                )
            }
        }
        ActionMenuItem(
            if (favorite) res.getString(L.ui_remove_from_favorites) else res.getString(L.ui_to_favorites),
            run { actions.setFavorite(row, !favorite) },
            icon = if (favorite) UiIconKind.BOOKMARK_FILLED else UiIconKind.BOOKMARK_ADD,
        )
        ActionMenuItem(
            res.getString(L.ui_copy_link),
            run { actions.copyLink(row.address) },
            icon = UiIconKind.LINK,
        )
    }
}
