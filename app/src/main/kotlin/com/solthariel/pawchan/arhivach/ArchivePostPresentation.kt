package com.solthariel.pawchan.arhivach

import com.solthariel.pawchan.core.ui.visiblePostSubject
import com.solthariel.pawchan.domain.archive.ArchiveMedia
import com.solthariel.pawchan.domain.archive.ArchivePost
import com.solthariel.pawchan.domain.archive.ArchiveThread
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.media.MediaAttachmentOrigin
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaGalleryContent
import com.solthariel.pawchan.media.PostMediaDisplay
import com.solthariel.pawchan.media.toAttachmentViewer
import com.solthariel.pawchan.media.toMediaGalleryContent
import com.solthariel.pawchan.thread.PreparedPostText
import com.solthariel.pawchan.thread.preparePostText

internal data class ArchivePostCardUi(
    val ordinal: Int,
    val post: ArchivePost,
    val media: PostMediaDisplay,
    val subject: String? = null,
    val document: PostDocument = PostDocument.plain(post.text),
    val richText: PreparedPostText = preparePostText(document),
)

internal data class ArchiveThreadPresentationUi(
    val posts: List<ArchivePostCardUi>,
    val media: List<MediaAttachmentUi>,
    val byNumber: Map<Long, ArchivePostCardUi>,
    val replies: Map<Long, List<ArchivePostCardUi>>,
    val gallery: MediaGalleryContent,
)

internal fun ArchiveThread.toArchiveThreadPresentationUi(): ArchiveThreadPresentationUi {
    val uniqueNumbers =
        posts
            .mapNotNull { it.postNumber() }
            .groupingBy { it }
            .eachCount()
            .filterValues { it == 1 }
            .keys
    val opNumber =
        posts
            .firstOrNull()
            ?.takeIf { it.details.originalPoster }
            ?.postNumber()
            ?.takeIf { it in uniqueNumbers }
    val cards = posts.mapIndexed { index, post -> post.toCard(index + 1, uniqueNumbers, opNumber, id) }
    val byNumber =
        cards
            .mapNotNull { card ->
                card.post
                    .postNumber()
                    ?.takeIf { it in uniqueNumbers }
                    ?.let { it to card }
            }.toMap()
    val replies =
        cards
            .flatMap { card ->
                card.document.runs
                    .mapNotNull { (it.link as? PostLink.Reference)?.post?.value }
                    .distinct()
                    .map { it to card }
            }.groupBy(keySelector = { it.first }, valueTransform = { it.second })
    val media = cards.flatMap { it.media.attachments }
    return ArchiveThreadPresentationUi(cards, media, byNumber, replies, media.toMediaGalleryContent())
}

private fun ArchivePost.postNumber(): Long? = number?.toLongOrNull()?.let(PostId::parse)?.value

private fun ArchivePost.toCard(
    ordinal: Int,
    validNumbers: Set<Long>,
    originalPostId: Long?,
    archiveThreadId: Long,
): ArchivePostCardUi {
    val original = document ?: PostDocument.plain(text)
    val safe =
        PostDocument(
            original.runs.map { run ->
                val reference = run.link as? PostLink.Reference
                if (reference != null && (reference.thread != null || reference.post.value !in validNumbers)) {
                    run.copy(link = null)
                } else {
                    run
                }
            },
        )
    return ArchivePostCardUi(
        ordinal = ordinal,
        post = this,
        media = PostMediaDisplay(media.map { it.toMediaAttachmentUi().withArchiveOrigin(archiveThreadId) }),
        subject = visiblePostSubject(subject, safe.plainText),
        document = safe,
        richText = preparePostText(safe, originalPostId),
    )
}

internal fun ArchiveMedia.toMediaAttachmentUi(): MediaAttachmentUi {
    val fileName = url.substringBefore('?').substringAfterLast('/')
    val extension = fileName.substringAfterLast('.', "").lowercase()
    val kind =
        when (extension) {
            "jpg", "jpeg", "png", "webp", "avif" -> MediaKind.IMAGE
            "gif" -> MediaKind.ANIMATION
            "webm", "mp4", "mkv" -> MediaKind.VIDEO
            "mp3", "ogg", "wav" -> MediaKind.AUDIO
            else -> MediaKind.FILE
        }
    val attachment =
        MediaAttachmentUi(
            source = MediaAttachmentSource(url, fileName, url, thumbnailUrl),
            byteSize = null,
            viewer = kind.toAttachmentViewer(),
            kind = kind,
            dimensions = details?.dimensions,
        )
    return attachment.copy(display = attachment.display.copy(byteSize = details?.sizeLabel))
}

internal fun MediaAttachmentUi.withArchiveOrigin(id: Long): MediaAttachmentUi =
    copy(source = source.copy(origin = MediaAttachmentOrigin(archiveThreadId = id)))
