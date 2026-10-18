package com.solthariel.pawchan.domain.model

enum class MediaKind {
    IMAGE,
    ANIMATION,
    VIDEO,
    AUDIO,
    FILE,
}

class MediaDimensions private constructor(
    val width: Int,
    val height: Int,
) {
    companion object {
        fun create(
            width: Int?,
            height: Int?,
        ): MediaDimensions? {
            val positiveWidth = width?.takeIf { it > 0 }
            val positiveHeight = height?.takeIf { it > 0 }
            return if (positiveWidth != null && positiveHeight != null) {
                MediaDimensions(positiveWidth, positiveHeight)
            } else {
                null
            }
        }
    }
}

data class AttachmentSource(
    val remoteUrl: RemoteUrl,
    val thumbnailUrl: RemoteUrl?,
)

data class MediaAttachment(
    val id: AttachmentId,
    val fileName: String,
    val source: AttachmentSource,
    val byteSize: ByteSize?,
    val kind: MediaKind,
    val dimensions: MediaDimensions? = null,
    val isSticker: Boolean = false,
) {
    val remoteUrl: RemoteUrl
        get() = source.remoteUrl

    val thumbnailUrl: RemoteUrl?
        get() = source.thumbnailUrl
}
