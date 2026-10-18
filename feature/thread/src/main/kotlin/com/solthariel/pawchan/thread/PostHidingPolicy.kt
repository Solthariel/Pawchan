package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.postpreferences.PostPreferences

internal fun hiddenPostIds(
    posts: List<ThreadPostUi>,
    rules: PostPreferences,
): Set<Long> {
    val trees = hiddenReplyTree(posts, rules.replyTrees)
    val seeds = rules.similar.map(::postWords)
    return posts
        .filter { post ->
            val words = if (seeds.isEmpty()) emptySet() else postWords(post.document.plainText)
            post.id !in rules.shown && (
                post.id in rules.hidden || post.id in trees ||
                    normalizedPostName(post.author) in rules.names ||
                    seeds.any { similarPostWords(it, words) }
            )
        }.map(ThreadPostUi::id)
        .toSet()
}

private fun hiddenReplyTree(
    posts: List<ThreadPostUi>,
    roots: Set<Long>,
): Set<Long> {
    val children = mutableMapOf<Long, MutableList<Long>>()
    posts.forEach { post ->
        post.referencedPostIds.forEach { parent -> children.getOrPut(parent) { mutableListOf() }.add(post.id) }
    }
    val hidden = roots.toMutableSet()
    val queue = ArrayDeque(roots)
    while (queue.isNotEmpty()) {
        children[queue.removeFirst()].orEmpty().forEach { child ->
            if (hidden.add(child)) queue.addLast(child)
        }
    }
    return hidden
}

internal fun normalizedPostName(name: String?): String =
    name
        ?.trim()
        ?.lowercase()
        .orEmpty()
        .ifBlank { "аноним" }

private fun postWords(text: String): Set<String> =
    text
        .lowercase()
        .split(WORD_BOUNDARY)
        .filter(String::isNotBlank)
        .toSet()

private fun similarPostWords(
    first: Set<String>,
    second: Set<String>,
): Boolean {
    if (first.isEmpty() || second.isEmpty()) return false
    val common = first.intersect(second).size
    val union = first.size + second.size - common
    return common.toDouble() / union >= SIMILARITY_THRESHOLD
}

private val WORD_BOUNDARY = Regex("[^\\p{L}\\p{N}]+")
private const val SIMILARITY_THRESHOLD = 0.8
