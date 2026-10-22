package com.solthariel.pawchan.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.archive.ArchiveCopyUi
import com.solthariel.pawchan.catalog.CatalogMediaClicks
import com.solthariel.pawchan.catalog.CatalogThreadCard
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.AppMenuDialog
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import com.solthariel.pawchan.workspace.WorkspaceBinding

/** Общее для карточек списка: загрузка миниатюр и просмотр файлов ОП-поста по нажатию на превью. */
internal data class LibraryCardContext(
    val thumbnails: ThumbnailLoading,
    val openMedia: (List<MediaAttachmentUi>) -> Unit,
) {
    val media: CatalogMediaClicks
        get() = CatalogMediaClicks(openMedia) {}
}

/** Сохранённая копия — карточка каталога; долгое нажатие — «Обновить», «ZIP», «Удалить» (без подтверждения). */
@Composable
internal fun SavedCopyItem(
    item: ArchiveCopyUi,
    binding: WorkspaceBinding,
    context: LibraryCardContext,
) {
    val controller = binding.archives ?: return
    val address = item.copy.snapshot.address
    var menu by remember(address) { mutableStateOf(false) }
    val export =
        com.solthariel.pawchan.archive
            .archiveFileExport(controller, address)
    val open: (ThreadAddress) -> Unit = {
        binding.controller.offlineAddress = address
        binding.navigate(address)
    }
    CatalogThreadCard(item.card, context.thumbnails, open, context.media, onThreadLongClick = { menu = true })
    if (menu) {
        val close = { menu = false }
        LibraryActionsDialog(close) {
            ActionMenuItem("Обновить", {
                close()
                controller.update(address)
            })
            ActionMenuItem("ZIP", {
                close()
                export()
            })
            ActionMenuItem("Удалить", {
                close()
                controller.remove(address)
            })
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
