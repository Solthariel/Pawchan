package com.solthariel.pawchan.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader

/**
 * Галерея внутри экрана (Избранное → Медиа): фильтры у правого края, как в галерее треда, и кнопка
 * выделения; в режиме выделения — «Все / Выбрано: N / Отмена» сверху и действия снизу.
 */
@Composable
fun MediaGalleryPane(
    gallery: MediaGalleryContent,
    loader: ThumbnailLoader,
    actions: MediaGalleryPaneActions,
    options: MediaGalleryPaneOptions,
    modifier: Modifier = Modifier,
    session: MediaGallerySession = rememberMediaGallerySession(),
) {
    val group = session.filter.group(gallery)
    Column(modifier) {
        if (session.selecting) {
            GallerySelectionToolbar(
                GalleryToolbarState(
                    session.filter,
                    selecting = true,
                    session.selected.size,
                    group.keys.isNotEmpty() && session.selected.containsAll(group.keys),
                ),
                GalleryToolbarActions(
                    back = session::clearSelection,
                    chooseFilter = { session.filter = it },
                    toggleSelection = session::toggleSelection,
                    selectAll = { session.selectAll(group.keys) },
                ),
            )
        } else {
            GalleryPaneFilters(gallery, session)
        }
        if (group.count == 0) {
            Text(options.emptyText, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            GalleryGrid(
                group,
                loader,
                session,
                GalleryCellActions(select = session::toggle, open = actions.onOpen, menu = actions.onLongClick),
                Modifier.weight(1f),
                if (session.selecting) 0.dp else options.bottomInset,
            )
        }
        if (session.selecting) {
            Column(Modifier.padding(bottom = options.bottomInset)) {
                val barActions = rememberPaneSelectionActions(gallery, actions, session)
                GallerySelectionBar(session.selected.isNotEmpty(), barActions)
            }
        }
    }
}

@Composable
private fun GalleryPaneFilters(
    gallery: MediaGalleryContent,
    session: MediaGallerySession,
) {
    Row(
        Modifier.fillMaxWidth().height(PANE_FILTERS_HEIGHT).padding(end = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GalleryFilter.entries.forEach { filter ->
            GalleryFilterButton(filter, filter.group(gallery).count, filter == session.filter) {
                session.filter = filter
            }
        }
        GalleryIconButton("Выделить медиа", session::toggleSelection) {
            GallerySelectIcon(MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun rememberPaneSelectionActions(
    gallery: MediaGalleryContent,
    actions: MediaGalleryPaneActions,
    session: MediaGallerySession,
): GallerySelectionBarActions {
    val context = LocalContext.current
    val chosen = { gallery.all.selectedItems(session.selected) }
    return GallerySelectionBarActions(
        download =
            actions.onDownloadAll?.let { download ->
                {
                    download(chosen())
                    session.clearSelection()
                }
            },
        copyLinks = {
            copyGalleryLinks(context, chosen())
            session.clearSelection()
        },
        favorite = null,
        remove =
            actions.onRemoveAll?.let { remove ->
                {
                    remove(chosen())
                    session.clearSelection()
                }
            },
    )
}

data class MediaGalleryPaneActions(
    val onOpen: (MediaAttachmentUi) -> Unit,
    val onLongClick: ((MediaAttachmentUi) -> Unit)? = null,
    /** «Скачать» в режиме выделения; null — кнопки нет. */
    val onDownloadAll: ((List<MediaAttachmentUi>) -> Unit)? = null,
    /** «Убрать» (из избранного) в режиме выделения; null — кнопки нет. */
    val onRemoveAll: ((List<MediaAttachmentUi>) -> Unit)? = null,
)

data class MediaGalleryPaneOptions(
    val emptyText: String,
    val bottomInset: Dp = 0.dp,
)

private val PANE_FILTERS_HEIGHT = 48.dp
