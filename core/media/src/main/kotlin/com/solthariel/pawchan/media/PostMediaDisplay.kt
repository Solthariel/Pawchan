package com.solthariel.pawchan.media

/** Prepared alongside the post UI model, never inside a scrolling item's composition. */
class PostMediaDisplay(
    val attachments: List<MediaAttachmentUi>,
    val rows: List<List<MediaAttachmentUi>> = attachments.chunked(MEDIA_ATTACHMENT_COLUMNS),
) {
    val stickers: List<MediaAttachmentUi> = attachments.filter { it.isSticker }
    val withoutStickers: List<MediaAttachmentUi> = attachments.filterNot { it.isSticker }
    val rowsWithoutStickers: List<List<MediaAttachmentUi>> = withoutStickers.chunked(MEDIA_ATTACHMENT_COLUMNS)

    fun visible(hideStickers: Boolean): List<MediaAttachmentUi> = if (hideStickers) withoutStickers else attachments

    fun visibleRows(hideStickers: Boolean): List<List<MediaAttachmentUi>> =
        if (hideStickers) {
            rowsWithoutStickers
        } else {
            rows
        }
}
