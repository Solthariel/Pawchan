package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.AttachmentSource
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.RemoteUrl
import com.solthariel.pawchan.domain.model.ThreadSnapshot

internal fun ThreadSnapshot.archived(responsePath: String): ThreadSnapshot {
    val prefix = "/${address.board.board.value}/"
    val archivePath = responsePath.substringBefore("/res/", "")
    val datedArchive = archivePath.takeIf { it.startsWith("${prefix}arch/") }
    val archivePosts =
        posts.map { post ->
            BoardPost(
                post.address,
                post.content,
                post.attachments.map { attachment ->
                    attachment.copy(
                        source =
                            AttachmentSource(
                                attachment.remoteUrl.inArchive(prefix, datedArchive),
                                attachment.thumbnailUrl?.inArchive(prefix, datedArchive),
                            ),
                    )
                },
            )
        }
    return ThreadSnapshot(
        address,
        subject,
        flags.copy(archived = true, closed = true),
        archivePosts,
        uniquePosters,
        subjectsEnabled,
    )
}

private fun RemoteUrl.inArchive(
    prefix: String,
    archivePath: String?,
): RemoteUrl {
    if (archivePath == null) return this
    val sourcePrefix = "$DVACH_ORIGIN$prefix"
    val suffix = value.removePrefix(sourcePrefix)
    return if (value.startsWith(sourcePrefix) && (suffix.startsWith("src/") || suffix.startsWith("thumb/"))) {
        requireNotNull(RemoteUrl.parse("$DVACH_ORIGIN$archivePath/$suffix"))
    } else {
        this
    }
}
