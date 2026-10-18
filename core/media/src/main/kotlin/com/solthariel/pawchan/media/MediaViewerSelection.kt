package com.solthariel.pawchan.media

data class MediaViewerSelection(
    val attachment: MediaAttachmentUi?,
    val items: List<MediaAttachmentUi>,
    val onSelect: (MediaAttachmentUi) -> Unit,
) {
    fun navigation(): MediaViewerNavigation {
        val index =
            items.indexOfFirst {
                attachment != null && it.originalUrl == attachment.originalUrl && it.id == attachment.id &&
                    it.source.ownerPostId == attachment.source.ownerPostId
            }
        val previous = items.getOrNull(index - 1)?.takeIf { index > 0 }
        val next = items.getOrNull(index + 1)?.takeIf { index >= 0 }
        return MediaViewerNavigation(
            previous = previous?.let { { onSelect(it) } },
            next = next?.let { { onSelect(it) } },
            position = if (index < 0) 1 else index + 1,
            total = if (index < 0) 1 else items.size,
            previousItem = previous,
            nextItem = next,
        )
    }
}

data class MediaViewerNavigation(
    val previous: (() -> Unit)? = null,
    val next: (() -> Unit)? = null,
    val position: Int = 1,
    val total: Int = 1,
    val previousItem: MediaAttachmentUi? = null,
    val nextItem: MediaAttachmentUi? = null,
)
