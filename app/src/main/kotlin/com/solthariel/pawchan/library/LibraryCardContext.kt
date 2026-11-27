package com.solthariel.pawchan.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.archive.ArchiveCopyUi
import com.solthariel.pawchan.catalog.CatalogThreadCard
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppMenuDialog
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import com.solthariel.pawchan.navigation.AppRoute
import com.solthariel.pawchan.threads.ThreadListCardActions
import com.solthariel.pawchan.workspace.WorkspaceBinding
import com.solthariel.pawchan.R.string as L

/** Общее для карточек списка: загрузка миниатюр и просмотр файлов ОП-поста по нажатию на превью. */
internal data class LibraryCardContext(
    val thumbnails: ThumbnailLoading,
    val openMedia: (List<MediaAttachmentUi>) -> Unit,
)

/** Карточка сохранённой копии с действиями по долгому нажатию. */
@Composable
internal fun SavedCopyItem(
    item: ArchiveCopyUi,
    binding: WorkspaceBinding,
    context: LibraryCardContext,
    zipRoutes: Set<String>,
) {
    val res = LocalResources.current

    val controller = binding.archives ?: return
    val address = item.copy.snapshot.address
    var menu by remember(address) { mutableStateOf(false) }
    val export =
        com.solthariel.pawchan.archive
            .archiveFileExport(controller, address)
    val open: () -> Unit = {
        binding.controller.offlineAddress = address
        binding.navigate(address)
    }
    val card =
        remember(item.card, zipRoutes) {
            if (AppRoute.thread(address) in zipRoutes) {
                val note = listOfNotNull(item.card.note, "ZIP").joinToString(" | ")
                item.card.copy(content = item.card.content.copy(note = note))
            } else {
                item.card
            }
        }
    CatalogThreadCard(
        card,
        context.thumbnails,
        actions =
            ThreadListCardActions(
                onClick = open,
                onOpenMedia = { attachments, _ -> context.openMedia(attachments) },
                onMediaLongClick = {},
                onLongClick = { menu = true },
            ),
        sizeText = card.statistics.sizeText,
    )
    if (menu) {
        val close = { menu = false }
        LibraryActionsDialog(close) {
            ActionMenuItem(res.getString(L.ui_refresh), {
                close()
                controller.update(address)
            }, icon = UiIconKind.REFRESH)
            ActionMenuItem(res.getString(L.ui_save_zip), {
                close()
                export()
            }, icon = UiIconKind.ARCHIVE)
            ActionMenuItem(res.getString(L.ui_delete), {
                close()
                controller.remove(address)
            }, icon = UiIconKind.DELETE)
        }
    }
}

@Composable
private fun LibraryActionsDialog(
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    AppMenuDialog(onClose) { content() }
}
