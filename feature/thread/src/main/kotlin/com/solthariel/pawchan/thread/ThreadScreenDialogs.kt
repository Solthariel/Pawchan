package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaGallery
import com.solthariel.pawchan.media.MediaGalleryActions
import com.solthariel.pawchan.media.MediaMenuRequest

@Composable
internal fun ThreadScreenDialogs(
    content: ThreadUiState.Content,
    tools: ThreadToolsState,
    bindings: ThreadDialogBindings,
    gallery: ThreadGalleryBindings,
    onOpen: (MediaAttachmentUi) -> Unit,
) {
    val close = { tools.dialog = ThreadDialog.NONE }
    when (tools.dialog) {
        ThreadDialog.NONE -> {
            Unit
        }

        ThreadDialog.APPEARANCE -> {
            ThreadAppearanceDialog(bindings.appearance, bindings.onAppearance, close)
        }

        ThreadDialog.GALLERY -> {
            MediaGallery(
                content.presentation.gallery,
                bindings.thumbnails,
                MediaGalleryActions(close, onOpen, gallery.onMenu, gallery.onDownloadAll, gallery.onFavoriteAll),
                gallery.session,
            )
        }

        ThreadDialog.ARCHIVE -> {
            bindings.archive.copyDialog(content.address, close)
        }

        ThreadDialog.ARHIVACH -> {
            val address = content.address
            if (address != null) {
                bindings.archive.external
                    ?.submitDialog
                    ?.invoke(address, close)
            }
        }

        ThreadDialog.HIDDEN_RULES -> {
            bindings.hiddenRules?.let { HiddenPostRulesDialog(it, close) }
        }

        ThreadDialog.REPORT -> {
            bindings.reports?.let {
                ThreadReportDialog(it, tools.reportTargets ?: tools.selected, close)
            }
        }
    }
}

internal data class ThreadGalleryBindings(
    val session: com.solthariel.pawchan.media.MediaGallerySession,
    val onMenu: (MediaMenuRequest) -> Unit,
    val onDownloadAll: (List<MediaAttachmentUi>) -> Unit,
    val onFavoriteAll: ((List<MediaAttachmentUi>) -> Unit)? = null,
)
