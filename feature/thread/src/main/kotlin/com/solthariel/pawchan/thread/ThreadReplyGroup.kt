package com.solthariel.pawchan.thread

import com.solthariel.pawchan.core.ui.russianPlural

internal data class ThreadReplyGroup(
    val posts: List<ThreadPostUi>,
) {
    val label: String = russianPlural(posts.size, "ответ", "ответа", "ответов")
}

internal fun prepareThreadReplyIndex(
    posts: List<ThreadPostUi>,
    address: com.solthariel.pawchan.domain.model.ThreadAddress? = null,
): Map<Long, ThreadReplyGroup> =
    posts
        .flatMap { post -> post.referencedPostIds.map { referenced -> referenced to post } }
        .groupBy(keySelector = Pair<Long, ThreadPostUi>::first, valueTransform = Pair<Long, ThreadPostUi>::second)
        .mapValues { (source, replies) ->
            val context = address?.let { ReplyReferenceContext(it, source) }
            ThreadReplyGroup(
                if (context == null) {
                    replies
                } else {
                    replies.map { it.copy(richText = it.richText.withReplyContext(context)) }
                },
            )
        }
