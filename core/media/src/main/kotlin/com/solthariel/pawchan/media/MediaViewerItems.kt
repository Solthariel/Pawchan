package com.solthariel.pawchan.media

fun List<MediaAttachmentUi>.withMediaOwner(
    postId: Long,
    prefix: String? = null,
): List<MediaAttachmentUi> = map { it.copy(source = it.source.copy(ownerPostId = postId, downloadPrefix = prefix)) }

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
