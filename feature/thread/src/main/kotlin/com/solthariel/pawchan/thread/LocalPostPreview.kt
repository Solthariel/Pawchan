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
            local.presentation.postPositions[target.postId]
                ?.let(local.posts::getOrNull)
                ?.takeIf { it.id == target.postId }
                ?.let(::listOf)
        }
    return posts?.let { PostPreviewState(content = local, posts = it) }
}
