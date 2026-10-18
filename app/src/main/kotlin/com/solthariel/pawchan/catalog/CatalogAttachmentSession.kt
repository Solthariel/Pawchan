package com.solthariel.pawchan.catalog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.media.MediaAttachmentUi

internal class CatalogAttachmentSession {
    var choices by mutableStateOf<List<MediaAttachmentUi>?>(null)
        private set

    fun open(attachments: List<MediaAttachmentUi>) {
        choices = attachments
    }

    fun openOrChoose(
        attachments: List<MediaAttachmentUi>,
        onOpen: (MediaAttachmentUi) -> Unit,
    ) {
        when (attachments.size) {
            0 -> {
                Unit
            }

            1 -> {
                choices = null
                onOpen(attachments.single())
            }

            else -> {
                open(attachments)
            }
        }
    }

    fun select(
        attachment: MediaAttachmentUi,
        onOpen: (MediaAttachmentUi) -> Unit,
    ) {
        if (choices?.contains(attachment) == true) onOpen(attachment)
    }

    fun dismiss() {
        choices = null
    }

    /** Выбор остаётся под просмотрщиком: тот открывается в отдельном окне поверх. */
    val visible: Boolean get() = choices != null
}
