package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.filter.ContentFilterRule

/** Refresh may update an open preview, but losing its source must not erase an already visible post. */
internal fun retainPreview(
    current: PostPreviewState,
    replacement: PostPreviewState,
): PostPreviewState {
    if (current.content == null || current.posts.isEmpty() || replacement.posts.isNotEmpty()) return replacement
    val omitted =
        replacement.content
            ?.presentation
            ?.contentFilters
            ?.omittedPosts
            .orEmpty()
    return if (current.posts.any { it.id in omitted }) replacement else current
}

internal fun PostPreviewState.withPreviewFilters(
    rules: List<ContentFilterRule>,
    showHiddenPosts: Boolean,
): PostPreviewState {
    val source = if (remote) content else null
    val filtered =
        source?.filteredContent(rules, showHiddenPosts = showHiddenPosts) as? ThreadUiState.Content ?: return this
    val omitted = filtered.presentation.contentFilters.omittedPosts
    val visible =
        posts.mapNotNull { post ->
            if (replies && post.id in omitted) null else filtered.postForPreview(post.id)
        }
    return copy(content = filtered, posts = visible)
}
