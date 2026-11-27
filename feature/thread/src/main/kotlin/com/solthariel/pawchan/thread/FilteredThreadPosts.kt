package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.filter.ContentFilterHit

internal data class FilteredThreadPosts(
    val posts: List<ThreadPostUi>,
    val hits: List<ContentFilterHit>,
    val omitted: Map<Long, ThreadPostUi>,
) {
    val matchedCount: Int get() = omitted.size + posts.count { it.metadata.contentFilterLabel != null }
}
