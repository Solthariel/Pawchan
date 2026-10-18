package com.solthariel.pawchan.threads

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppMenuDialog
import com.solthariel.pawchan.domain.model.ThreadAddress

/** Меню по долгому нажатию: убрать из секции, избранное, ссылка. */
@Composable
internal fun ThreadsRowMenu(
    target: ThreadsMenuTarget,
    favorites: Set<ThreadAddress>,
    actions: ThreadsScreenActions,
    onClose: () -> Unit,
) {
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
                ActionMenuItem("Закрыть", run { actions.pages.close(row.address) })
            }

            ThreadsSection.FAVORITES -> {
                Unit
            }

            ThreadsSection.REPLIES -> {
                ActionMenuItem("Убрать из ответов", run { actions.dismissReplies(row.address) })
            }
        }
        ActionMenuItem(
            if (favorite) "Убрать из избранного" else "В избранное",
            run { actions.setFavorite(row, !favorite) },
        )
        ActionMenuItem("Копировать ссылку", run { actions.copyLink(row.address) })
    }
}
