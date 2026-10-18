package com.solthariel.pawchan.thread

import com.solthariel.pawchan.core.ui.russianPlural

internal data class ThreadReplyGroup(
    val posts: List<ThreadPostUi>,
) {
    val label: String = russianPlural(posts.size, "ответ", "ответа", "ответов")
}

internal fun prepareThreadReplyIndex(posts: List<ThreadPostUi>): Map<Long, ThreadReplyGroup> =
    posts
        .flatMap { post -> post.referencedPostIds.map { referenced -> referenced to post } }
        .groupBy(keySelector = Pair<Long, ThreadPostUi>::first, valueTransform = Pair<Long, ThreadPostUi>::second)
        .mapValues { (_, replies) -> ThreadReplyGroup(replies) }
