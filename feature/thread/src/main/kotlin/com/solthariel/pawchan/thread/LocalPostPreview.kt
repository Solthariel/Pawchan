package com.solthariel.pawchan.thread

/** O(1) lookups into the prepared thread indices, safe for a preview's first frame. */
internal fun localPostPreview(
    target: PostPreviewTarget,
    local: ThreadUiState.Content?,
): PostPreviewState? {
    if (local == null || (target.address != null && target.address != local.address)) return null
    val posts =
        if (target.replies) {
            local.presentation.replyIndex[target.postId]
                ?.posts
                .orEmpty()
        } else {
            local.postForPreview(target.postId)?.let(::listOf)
        }
    return posts?.let { PostPreviewState(content = local, posts = it, replies = target.replies) }
}

/** Explicit links may open a collapsed post even if it has no scroll position in the list. */
internal fun ThreadUiState.Content.postForPreview(id: Long): ThreadPostUi? =
    presentation.postPositions[id]?.let(posts::getOrNull)?.takeIf { it.id == id }
        ?: presentation.contentFilters.omittedPosts[id]
