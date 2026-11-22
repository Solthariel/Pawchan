package com.solthariel.pawchan.media

import com.solthariel.pawchan.domain.model.ThreadAddress

fun List<MediaAttachmentUi>.withMediaOwner(
    postId: Long,
    prefix: String? = null,
    threadAddress: ThreadAddress? = null,
): List<MediaAttachmentUi> =
    map {
        it.copy(
            source =
                it.source.copy(
                    ownerPostId = postId,
                    downloadPrefix = prefix,
                    origin =
                        threadAddress?.let { address -> MediaAttachmentOrigin(threadAddress = address) }
                            ?: it.source.origin,
                ),
        )
    }

fun List<MediaAttachmentUi>.viewerItems(): List<MediaAttachmentUi> =
    filter { attachment ->
        attachment.viewer != AttachmentViewer.NONE
    }

fun postMediaSelection(
    attachment: MediaAttachmentUi?,
    groups: Map<Long, List<MediaAttachmentUi>>,
    onSelect: (MediaAttachmentUi) -> Unit,
): MediaViewerSelection {
    val group = groups[attachment?.source?.ownerPostId].orEmpty()
    val items = if (group.isEmpty() && attachment != null) listOf(attachment) else group
    return MediaViewerSelection(attachment, items, onSelect = onSelect)
}
