package com.solthariel.pawchan.domain.thread

import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot

/**
 * Локальная история просмотренных тредов: пост, пропавший из ответа
 * сервера, остаётся удалённым, пока его не уберут вручную.
 */
interface ThreadHistoryRepository {
    suspend fun load(address: ThreadAddress): ThreadSnapshot?

    suspend fun save(snapshot: ThreadSnapshot)

    suspend fun erase(address: ThreadAddress)
}

/**
 * Свежий ответ сервера + сохранённая история. Пропавшие посты помечаются удалёнными, вернувшиеся —
 * снова живые, изменённые берутся из ответа.
 */
fun mergeThreadHistory(
    previous: ThreadSnapshot?,
    incoming: ThreadSnapshot,
): ThreadSnapshot {
    val missing =
        if (previous != null && previous.address == incoming.address) {
            val live = incoming.posts.map { it.address.post }.toSet()
            previous.posts.filter { it.address.post !in live }
        } else {
            emptyList()
        }
    return if (missing.isEmpty()) {
        incoming
    } else {
        val posts = (incoming.posts + missing).sortedBy { it.address.post.value }
        incoming.withPosts(posts, missing.map { it.address.post }.toSet())
    }
}

/**
 * «Восстановить из Архивача»: посты копии, которых нет в истории, встают на свои места удалёнными.
 * Уже известные посты не перезаписываются.
 */
fun ThreadSnapshot.withRestoredPosts(archived: ThreadSnapshot): ThreadSnapshot {
    val known = posts.map { it.address.post }.toSet()
    val restored = archived.posts.filter { it.address.thread == address && it.address.post !in known }
    return if (restored.isEmpty()) {
        this
    } else {
        withPosts(
            (posts + restored).sortedBy { it.address.post.value },
            deletedPosts + restored.map { it.address.post },
        )
    }
}

/** «Очистить удалённые»: остаются только живые посты. */
fun ThreadSnapshot.withoutDeleted(): ThreadSnapshot = withPosts(posts.filter { it.address.post !in deletedPosts })

/**
 * В цикличном треде сервер отбрасывает старые посты, и они становятся удалёнными между ОП-постом
 * и первым живым. «Очистить старые» убирает именно их.
 */
fun ThreadSnapshot.withoutOldDeleted(): ThreadSnapshot {
    val old = oldDeletedPosts()
    return withPosts(posts.filter { it.address.post !in old }, deletedPosts - old)
}

private fun ThreadSnapshot.oldDeletedPosts() =
    posts
        .drop(1)
        .takeWhile { it.address.post in deletedPosts }
        .map { it.address.post }
        .toSet()
