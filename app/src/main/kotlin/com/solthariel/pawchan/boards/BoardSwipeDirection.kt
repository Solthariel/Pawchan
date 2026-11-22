package com.solthariel.pawchan.boards

import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.infrastructure.sites.ForumBoardSource

internal enum class BoardSwipeDirection {
    PREVIOUS,
    NEXT,
}

internal const val ARCHIVE_SOURCE_ID = "arhivach"

internal fun adjacentForumSource(
    current: SiteId,
    sources: List<ForumBoardSource>,
    direction: BoardSwipeDirection,
): ForumBoardSource? {
    val currentIndex = sources.indexOfFirst { it.site == current }
    if (currentIndex < 0) return null
    val adjacentIndex =
        currentIndex +
            when (direction) {
                BoardSwipeDirection.PREVIOUS -> -1
                BoardSwipeDirection.NEXT -> 1
            }
    return sources.getOrNull(adjacentIndex)
}

internal fun adjacentBoardSourceTab(
    current: String,
    sources: List<ForumBoardSource>,
    direction: BoardSwipeDirection,
): String? {
    val tabs = sources.map { it.site.value }
    val currentIndex = tabs.indexOf(current)
    if (currentIndex < 0) return null
    val adjacentIndex =
        currentIndex +
            when (direction) {
                BoardSwipeDirection.PREVIOUS -> -1
                BoardSwipeDirection.NEXT -> 1
            }
    return tabs.getOrNull(adjacentIndex)
}

internal fun boardForForum(
    current: BoardAddress,
    targetSite: SiteId,
    boards: List<BoardDirectoryEntry>,
): BoardAddress? {
    val ordered =
        boardCategories(boards.filter { it.address.site == targetSite })
            .flatMap { category -> category.boards }
    return ordered.firstOrNull { it.address.board == current.board }?.address
        ?: ordered.firstOrNull()?.address
}
