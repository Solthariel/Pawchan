package com.solthariel.pawchan.workspace

import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.toMediaAttachmentUi
import com.solthariel.pawchan.thread.SavedPostPresentation

internal data class WorkspacePostUi(
    val presentation: SavedPostPresentation,
    val address: ThreadAddress,
    val label: String,
    val title: String? = null,
    val newReplies: Int = 0,
) {
    val id: Long get() = presentation.address.post.value
}

internal data class WorkspacePrepared(
    val own: List<WorkspacePostUi> = emptyList(),
    val media: List<MediaAttachmentUi> = emptyList(),
    val unread: Int = 0,
)

internal fun prepareWorkspace(state: WorkspaceState): WorkspacePrepared {
    val groups =
        (
            state.posts.map {
                it.post
            } + state.replies.map { it.post }
        ).distinctBy { it.address }.groupBy { it.address.thread }
    val contents =
        groups.mapValues { (address, posts) ->
            val snapshot = ThreadSnapshot(address, null, ThreadFlags(false, false, false), posts)
            SavedPostPresentation.prepare(snapshot)
        }
    val prepared =
        state.posts.mapNotNull { stored ->
            val presentation =
                contents[stored.post.address.thread]?.get(stored.post.address.post.value)
                    ?: return@mapNotNull null
            val newReplies =
                state.replies.count { reply ->
                    reply.unread && reply.post.address.thread == stored.post.address.thread &&
                        reply.post.content.document.runs
                            .any { (it.link as? PostLink.Reference)?.post == stored.post.address.post }
                }
            stored to
                WorkspacePostUi(
                    presentation,
                    stored.post.address.thread,
                    "/${stored.post.address.thread.board.board.value}/ ${stored.title.orEmpty()}" +
                        if (newReplies > 0) " • новых: $newReplies" else "",
                    stored.title,
                    newReplies,
                )
        }
    val media = state.media.map { it.toMediaAttachmentUi() }
    return WorkspacePrepared(
        prepared.filter { it.first.own }.map { it.second },
        media,
        state.replies.count { it.unread },
    )
}
