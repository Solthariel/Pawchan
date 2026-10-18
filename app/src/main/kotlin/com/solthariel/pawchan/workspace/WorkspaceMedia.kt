package com.solthariel.pawchan.workspace

import com.solthariel.pawchan.domain.model.AttachmentId
import com.solthariel.pawchan.domain.model.AttachmentSource
import com.solthariel.pawchan.domain.model.ByteSize
import com.solthariel.pawchan.domain.model.MediaAttachment
import com.solthariel.pawchan.domain.model.RemoteUrl
import com.solthariel.pawchan.media.MediaAttachmentUi

internal fun MediaAttachmentUi.toDomainMedia(): MediaAttachment =
    MediaAttachment(
        requireNotNull(AttachmentId.parse(id)),
        fileName,
        AttachmentSource(requireNotNull(RemoteUrl.parse(originalUrl)), thumbnailUrl?.let(RemoteUrl::parse)),
        byteSize?.let(ByteSize::parse),
        kind,
        dimensions,
        isSticker = isSticker,
    )

/** Добавляет файлы в избранное без повторов (по адресу файла). */
internal fun WorkspaceController.addFavoriteMedia(files: List<MediaAttachmentUi>) {
    change { state ->
        val known = state.media.mapTo(HashSet()) { it.remoteUrl }
        val added = files.map { it.toDomainMedia() }.filter { known.add(it.remoteUrl) }
        if (added.isEmpty()) state else state.copy(media = state.media + added)
    }
}
