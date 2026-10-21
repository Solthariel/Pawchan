package com.solthariel.pawchan.thread

import com.solthariel.pawchan.core.ui.FastScrollMarkers

/** Built with the rest of post navigation off Main, not on every scroll/recomposition. */
internal fun prepareThreadScrollMarkers(posts: List<ThreadPostUi>): FastScrollMarkers {
    val own =
        posts.mapIndexedNotNull { index, post ->
            val marked = post.metadata.changes.own && !post.metadata.hidden
            index.takeIf { marked }
        }
    val replies =
        posts.mapIndexedNotNull { index, post ->
            val marked = post.metadata.changes.replyToMe && !post.metadata.hidden
            index.takeIf { marked }
        }
    return FastScrollMarkers(itemCount = posts.size, own = own, replies = replies)
}

internal fun ThreadUiState.Content.readingReturnScrollMarkers(point: ThreadReadingPosition?): FastScrollMarkers =
    presentation.navigation.scrollMarkers.copy(reading = point?.let { presentation.postPositions[it.postId] })
