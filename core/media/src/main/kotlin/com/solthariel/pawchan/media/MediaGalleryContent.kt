package com.solthariel.pawchan.media

import com.solthariel.pawchan.domain.model.MediaKind

data class MediaGalleryContent(
    val all: MediaGalleryGroup = MediaGalleryGroup(),
    val images: MediaGalleryGroup = MediaGalleryGroup(),
    val animations: MediaGalleryGroup = MediaGalleryGroup(),
    val videos: MediaGalleryGroup = MediaGalleryGroup(),
    val withoutStickers: MediaGalleryContent? = null,
)

data class MediaGalleryGroup(
    val count: Int = 0,
    val rows: List<List<MediaAttachmentUi>> = emptyList(),
    val items: List<MediaAttachmentUi> = emptyList(),
    /** Уникальный ключ каждого элемента items: один файл (md5) может быть в нескольких постах. */
    val keys: List<String> = emptyList(),
) {
    fun keyAt(index: Int): String = keys.getOrNull(index) ?: "gallery-index-$index"
}

/** Prepare on the caller's background dispatcher before publishing UI state. */
fun List<MediaAttachmentUi>.toMediaGalleryContent(): MediaGalleryContent {
    val attachments = filter { it.viewer != AttachmentViewer.NONE }.withGalleryKeys()
    return attachments.toGalleryContent().copy(
        withoutStickers = attachments.filterNot { it.first.isSticker }.toGalleryContent(),
    )
}

private fun List<Pair<MediaAttachmentUi, String>>.toGalleryContent(): MediaGalleryContent =
    MediaGalleryContent(
        all = toGalleryGroup(),
        images = filter { it.first.kind == MediaKind.IMAGE }.toGalleryGroup(),
        animations =
            filter { it.first.kind == MediaKind.ANIMATION }
                .toGalleryGroup(),
        videos = filter { it.first.kind == MediaKind.VIDEO }.toGalleryGroup(),
    )

/** Ключ = пост + id файла + номер повтора; id (md5) сам по себе не уникален в треде. */
internal fun List<MediaAttachmentUi>.withGalleryKeys(): List<Pair<MediaAttachmentUi, String>> {
    val seen = HashMap<String, Int>()
    return map { attachment ->
        val base = "${attachment.source.ownerPostId}/${attachment.id}"
        val repeat = seen[base] ?: 0
        seen[base] = repeat + 1
        attachment to if (repeat == 0) base else "$base#$repeat"
    }
}

private fun List<Pair<MediaAttachmentUi, String>>.toGalleryGroup(): MediaGalleryGroup {
    val items = map { it.first }
    return MediaGalleryGroup(
        count = size,
        rows = items.chunked(MEDIA_ATTACHMENT_COLUMNS),
        items = items,
        keys = map { it.second },
    )
}
