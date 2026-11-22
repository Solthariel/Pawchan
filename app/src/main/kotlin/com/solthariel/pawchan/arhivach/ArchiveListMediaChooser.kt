package com.solthariel.pawchan.arhivach

import com.solthariel.pawchan.catalog.CatalogAttachmentSession
import com.solthariel.pawchan.media.MediaAttachmentUi

internal data class ArchiveListMediaState(
    val attachment: MediaAttachmentUi,
    val items: List<MediaAttachmentUi>,
)

/** Mirrors catalog behavior: choose from a multi-file OP preview before opening the viewer. */
internal class ArchiveListMediaChooser {
    private val session = CatalogAttachmentSession()

    val choices: List<MediaAttachmentUi>? get() = session.choices
    val visible: Boolean get() = session.visible

    fun open(
        items: List<MediaAttachmentUi>,
        onOpen: (ArchiveListMediaState) -> Unit,
    ) {
        session.openOrChoose(items) { selected ->
            onOpen(ArchiveListMediaState(selected, items))
        }
    }

    fun select(
        attachment: MediaAttachmentUi,
        onOpen: (ArchiveListMediaState) -> Unit,
    ) {
        val items = session.choices ?: return
        session.select(attachment) { selected ->
            onOpen(ArchiveListMediaState(selected, items))
        }
    }

    fun dismiss() = session.dismiss()
}
