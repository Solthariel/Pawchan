package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.filter.ContentFilterEngine
import com.solthariel.pawchan.domain.filter.ContentFilterPlace
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.settings.DEFAULT_SHOW_HIDDEN_POSTS

internal fun ThreadUiState.filteredContent(
    rules: List<ContentFilterRule>,
    revealed: Set<Long> = emptySet(),
    showHiddenPosts: Boolean = DEFAULT_SHOW_HIDDEN_POSTS,
): ThreadUiState {
    if (this !is ThreadUiState.Content) return this
    val board = address?.board
    return if (board == null) this else withContentFilters(board, rules, revealed, showHiddenPosts)
}

private fun ThreadUiState.Content.withContentFilters(
    board: BoardAddress,
    rules: List<ContentFilterRule>,
    revealed: Set<Long>,
    showHiddenPosts: Boolean,
): ThreadUiState.Content {
    val threadId = address?.thread?.value ?: id
    val engine = ContentFilterEngine(rules, ContentFilterPlace.THREAD)
    val threadRule =
        engine.thread(
            board,
            subject,
            posts
                .firstOrNull { it.id == id }
                ?.document
                ?.plainText
                .orEmpty(),
            threadId,
        )
    val filtered = filteredThreadPosts(engine, revealed, showHiddenPosts)
    val filteredPosts = filtered.posts
    return copy(
        posts = filteredPosts,
        presentation =
            presentation.copy(
                contentFilters =
                    ThreadContentFilterState(
                        notice = threadRule?.label,
                        hits = filtered.hits,
                        omittedPosts = filtered.omitted,
                        filteredComments = filtered.matchedCount,
                        samples =
                            posts.map { post ->
                                com.solthariel.pawchan.domain.filter.ContentFilterSample(
                                    board,
                                    subject,
                                    post.document.plainText,
                                    post.id != id,
                                    threadId = threadId,
                                )
                            },
                    ),
            ),
    ).withPostNavigation()
}
