package com.solthariel.pawchan.thread

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.solthariel.pawchan.core.ui.russianPlural

/** Что изменилось после обновления треда: как плашка Dashchan «10 новых постов · Показать». */
internal data class ThreadRefreshNotice(
    val newPosts: Int,
    val replies: Int,
    val deleted: Int,
    val firstNew: Long?,
) {
    val message: String
        get() =
            listOfNotNull(
                newPosts.takeIf { it > 0 }?.let { russianPlural(it, "новый пост", "новых поста", "новых постов") },
                replies.takeIf { it > 0 }?.let { russianPlural(it, "ответ вам", "ответа вам", "ответов вам") },
                deleted.takeIf { it > 0 }?.let { russianPlural(it, "удалён", "удалено", "удалено") },
            ).joinToString(" · ")

    fun merge(next: ThreadRefreshNotice): ThreadRefreshNotice =
        ThreadRefreshNotice(
            newPosts = newPosts + next.newPosts,
            replies = replies + next.replies,
            deleted = deleted + next.deleted,
            firstNew = firstNew ?: next.firstNew,
        )
}

/**
 * Новые посты треда: плашка после обновления и подсветка, пока пост не показался на экране.
 * Сравнивает каждый новый список постов с предыдущим показанным; первый показ ничего не отмечает.
 */
internal class ThreadFreshPosts {
    private var previous: Map<Long, ThreadPostUi>? = null
    val unseen = mutableStateListOf<Long>()
    val notice = mutableStateOf<ThreadRefreshNotice?>(null)

    fun update(posts: List<ThreadPostUi>) {
        val prior = previous
        previous = posts.associateBy(ThreadPostUi::id)
        if (prior == null) return
        val fresh =
            posts.filter { post ->
                post.id !in prior && !post.metadata.changes.deleted && !post.metadata.changes.own
            }
        val deleted = posts.count { it.metadata.changes.deleted && prior[it.id]?.metadata?.changes?.deleted == false }
        val change =
            ThreadRefreshNotice(
                newPosts = fresh.size,
                replies = fresh.count { it.metadata.changes.replyToMe },
                deleted = deleted,
                firstNew = fresh.firstOrNull()?.id,
            )
        if (change.newPosts == 0 && change.deleted == 0) return
        unseen.addAll(fresh.map(ThreadPostUi::id).filterNot(unseen::contains))
        // Пока плашка видна, число в ней растёт на месте, а «Показать» ведёт к самому раннему новому посту.
        notice.value = notice.value?.let { shown -> shown.merge(change) } ?: change
    }

    fun seen(id: Long) {
        unseen.remove(id)
        // Новые посты долистали сами — плашка больше не нужна.
        if (unseen.isEmpty() && notice.value?.firstNew != null) dismiss()
    }

    fun dismiss() {
        notice.value = null
    }
}
