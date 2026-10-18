package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.toMediaGalleryContent
import com.solthariel.pawchan.media.viewerItems

/**
 * Отметки «новый» и «изменён» относительно прошлого показа. Удалённые посты и номера приходят из
 * истории треда (см. mergeThreadHistory): удалённый остаётся на месте, номер у него «X».
 */
internal fun ThreadUiState.withPostHistory(
    previous: ThreadUiState?,
    own: Set<Long>,
): ThreadUiState {
    if (this !is ThreadUiState.Content) return this
    val prior = (previous as? ThreadUiState.Content)?.posts.orEmpty().associateBy(ThreadPostUi::id)
    val prepared =
        posts.map { post ->
            val old = prior[post.id]
            val references = post.document.runs.mapNotNull { (it.link as? PostLink.Reference)?.post?.value }
            val deleted = post.metadata.changes.deleted
            val changes =
                ThreadPostChanges(
                    own = post.id in own,
                    replyToMe = references.any { it in own },
                    new = !deleted && prior.isNotEmpty() && old == null,
                    edited = !deleted && old != null && !old.metadata.changes.deleted && changedPost(post, old),
                    deleted = deleted,
                    originalPoster = post.metadata.display.originalPoster || post.id == id,
                )
            post.copy(metadata = post.metadata.copy(changes = changes))
        }
    return copy(posts = prepared).withPostNavigation()
}

private fun changedPost(
    current: ThreadPostUi,
    previous: ThreadPostUi,
): Boolean =
    current.document.plainText != previous.document.plainText ||
        current.attachments.map(MediaAttachmentUi::originalUrl) !=
        previous.attachments.map(MediaAttachmentUi::originalUrl) ||
        current.author != previous.author || current.metadata.display.subject != previous.metadata.display.subject

internal fun ThreadUiState.Content.withPostNavigation(): ThreadUiState.Content {
    val visible = posts.filterNot { it.metadata.hidden }
    return copy(
        presentation =
            presentation.copy(
                gallery = visible.flatMap(ThreadPostUi::attachments).toMediaGalleryContent(),
                navigation =
                    ThreadPostNavigation(
                        mediaGroups = visible.associate { it.id to it.attachments.viewerItems() },
                        replyIndex = prepareThreadReplyIndex(posts),
                        postPositions = posts.mapIndexed { index, post -> post.id to index }.toMap(),
                    ),
            ),
    )
}
