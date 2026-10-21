package com.solthariel.pawchan.domain.workspace

import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.MediaAttachment
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot

data class StoredPost(
    val post: BoardPost,
    val title: String?,
    val own: Boolean = false,
    val tracking: Boolean = true,
)

data class StoredReply(
    val post: BoardPost,
    val title: String?,
    val unread: Boolean = true,
)

data class OpenThread(
    val address: ThreadAddress,
    val title: String?,
    val postId: Long? = null,
    val offset: Int = 0,
)

data class ThreadCopy(
    val snapshot: ThreadSnapshot,
    val savedAt: Long,
    val archive: com.solthariel.pawchan.domain.archive.ArchiveState =
        com.solthariel.pawchan.domain.archive
            .ArchiveState(),
    val position: OpenThread? = null,
)

data class WorkspaceState(
    val posts: List<StoredPost> = emptyList(),
    val replies: List<StoredReply> = emptyList(),
    val tabs: List<OpenThread> = emptyList(),
    val media: List<MediaAttachment> = emptyList(),
    val copies: List<ThreadCopy> = emptyList(),
    val lastRoute: String? = null,
    val marks: ThreadMarks = ThreadMarks(),
)

/** Пометки тредов по адресу: скрытые в каталоге и известные копии на Архиваче. */
data class ThreadMarks(
    val hidden: Set<ThreadAddress> = emptySet(),
    /** Копии тредов на Архиваче, добавленные из приложения. */
    val arhivach: Map<ThreadAddress, String> = emptyMap(),
    /** Последний пост, который был в треде при последнем заходе: от него считаются новые посты. */
    val seen: Map<ThreadAddress, Long> = emptyMap(),
    /** Треды, убранные из «Ответов» вручную; новый свой пост возвращает тред обратно. */
    val dismissedReplies: Set<ThreadAddress> = emptySet(),
    /** Independent return anchors; normal scrolling never moves these. */
    val readingReturns: Map<ThreadAddress, ReadingReturnPoint> = emptyMap(),
)

/** Запоминает последний пост треда; держит не больше [SEEN_THREADS_LIMIT] тредов, старые отбрасываются. */
fun WorkspaceState.withSeen(
    address: ThreadAddress,
    lastPostId: Long,
): WorkspaceState {
    if (marks.seen[address] == lastPostId) return this
    val seen = (marks.seen - address).entries.toList().takeLast(SEEN_THREADS_LIMIT - 1)
    return copy(marks = marks.copy(seen = seen.associate { it.key to it.value } + (address to lastPostId)))
}

/** Убрать тред из «Ответов» или вернуть его. */
fun WorkspaceState.withRepliesDismissed(
    address: ThreadAddress,
    dismissed: Boolean,
): WorkspaceState =
    copy(
        marks =
            marks.copy(
                dismissedReplies =
                    if (dismissed) marks.dismissedReplies + address else marks.dismissedReplies - address,
            ),
    )

/** Зашли в тред — все ответы в нём прочитаны, а новые посты отсчитываются от последнего. */
fun WorkspaceState.visit(snapshot: ThreadSnapshot): WorkspaceState {
    val last = snapshot.posts.maxOfOrNull { it.address.post.value }
    val read =
        copy(
            replies =
                replies.map { reply ->
                    val inThread = reply.post.address.thread == snapshot.address
                    if (reply.unread && inThread) reply.copy(unread = false) else reply
                },
        )
    return if (last == null) read else read.withSeen(snapshot.address, last)
}

const val SEEN_THREADS_LIMIT = 500

fun WorkspaceState.withHiddenThread(
    address: ThreadAddress,
    hide: Boolean,
): WorkspaceState = copy(marks = marks.copy(hidden = if (hide) marks.hidden + address else marks.hidden - address))

fun WorkspaceState.withArhivachLink(
    address: ThreadAddress,
    url: String,
): WorkspaceState = copy(marks = marks.copy(arhivach = marks.arhivach + (address to url)))

interface WorkspaceRepository {
    suspend fun load(): WorkspaceState

    suspend fun update(transform: (WorkspaceState) -> WorkspaceState): WorkspaceState
}

fun WorkspaceState.storePost(
    post: BoardPost,
    title: String?,
    own: Boolean = false,
): WorkspaceState {
    val previous = posts.firstOrNull { it.post.address == post.address }
    val stored = StoredPost(post, title, own || previous?.own == true, tracking = previous?.tracking ?: true)
    val updated = copy(posts = posts.filterNot { it.post.address == post.address } + stored)
    return if (own) updated.withRepliesDismissed(post.address.thread, false) else updated
}

fun WorkspaceState.observe(snapshot: ThreadSnapshot): WorkspaceState {
    val own = posts.filter { it.own && it.post.address.thread == snapshot.address }.map { it.post.address.post }.toSet()
    val found =
        snapshot.posts.filter { post ->
            post.address.post !in own &&
                post.content.document.runs.any { run ->
                    val reference = run.link as? com.solthariel.pawchan.domain.model.PostLink.Reference
                    reference?.post in own && (reference?.thread == null || reference.thread == snapshot.address)
                }
        }
    val updated =
        found.map { post ->
            val previous = replies.firstOrNull { it.post.address == post.address }
            StoredReply(post, snapshot.subject, previous?.unread ?: true)
        }
    val refreshed =
        posts.map { stored ->
            snapshot.posts.firstOrNull { it.address == stored.post.address }?.let {
                stored.copy(
                    post = it,
                    title = snapshot.subject,
                    tracking =
                        stored.tracking && !snapshot.flags.closed && !snapshot.flags.archived,
                )
            }
                ?: stored
        }
    return copy(
        posts = refreshed,
        replies =
            replies.filterNot { old -> updated.any { it.post.address == old.post.address } } + updated,
    )
}

fun WorkspaceState.markRead(addresses: Set<PostAddress>): WorkspaceState =
    copy(replies = replies.map { if (it.post.address in addresses) it.copy(unread = false) else it })
