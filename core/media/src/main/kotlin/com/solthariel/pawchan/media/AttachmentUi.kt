package com.solthariel.pawchan.media

import com.solthariel.pawchan.core.ui.formatByteSizeForUi
import com.solthariel.pawchan.domain.model.MediaAttachment
import com.solthariel.pawchan.domain.model.MediaDimensions
import com.solthariel.pawchan.domain.model.MediaKind

data class MediaAttachmentUi(
    val source: MediaAttachmentSource,
    val byteSize: Long?,
    val viewer: AttachmentViewer,
    val kind: MediaKind = MediaKind.FILE,
    val dimensions: MediaDimensions? = null,
    val isSticker: Boolean = false,
    val display: MediaAttachmentDisplay =
        MediaAttachmentDisplay(
            format = attachmentFormat(source.originalUrl, source.fileName),
            byteSize = byteSize?.let(::formatByteSizeForUi),
            dimensions = dimensions?.let { "${it.width}×${it.height}" },
        ),
) {
    val id: String
        get() = source.id
    val fileName: String
        get() = source.fileName
    val originalUrl: String
        get() = source.originalUrl
    val thumbnailUrl: String?
        get() = source.thumbnailUrl
    val displayFormat: String
        get() = display.format
    val displayByteSize: String?
        get() = display.byteSize
    val displayDimensions: String?
        get() = display.dimensions
}

data class MediaAttachmentDisplay(
    val format: String,
    val byteSize: String?,
    val dimensions: String?,
) {
    val typeAndSize: String = if (byteSize == null) format else "$format $byteSize"
}

data class MediaAttachmentSource(
    val id: String,
    val fileName: String,
    val originalUrl: String,
    val thumbnailUrl: String?,
    val ownerPostId: Long? = null,
    val downloadPrefix: String? = null,
)

enum class AttachmentViewer {
    IMAGE,
    VIDEO,
    NONE,
}

fun MediaAttachment.toMediaAttachmentUi(): MediaAttachmentUi =
    MediaAttachmentUi(
        source =
            MediaAttachmentSource(
                id = id.value,
                fileName = fileName,
                originalUrl = remoteUrl.value,
                thumbnailUrl = thumbnailUrl?.value,
            ),
        byteSize = byteSize?.value,
        viewer = kind.toAttachmentViewer(),
        kind = kind,
        dimensions = dimensions,
        isSticker = isSticker,
    )

fun MediaKind.toAttachmentViewer(): AttachmentViewer =
    when (this) {
        MediaKind.IMAGE, MediaKind.ANIMATION -> AttachmentViewer.IMAGE
        MediaKind.VIDEO -> AttachmentViewer.VIDEO
        MediaKind.AUDIO, MediaKind.FILE -> AttachmentViewer.NONE
    }
