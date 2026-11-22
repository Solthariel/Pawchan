package com.solthariel.pawchan

import com.solthariel.pawchan.boards.ARCHIVE_SOURCE_ID
import com.solthariel.pawchan.boards.BoardSwipeDirection
import com.solthariel.pawchan.boards.adjacentBoardSourceTab
import com.solthariel.pawchan.boards.adjacentForumSource
import com.solthariel.pawchan.boards.boardForForum
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.infrastructure.sites.ForumBoardSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AdjacentForumNavigationTest {
    private val dvach = requireNotNull(SiteId.parse("dvach"))
    private val fourChan = requireNotNull(SiteId.parse("4chan"))
    private val directory =
        object : BoardDirectoryRepository {
            override suspend fun loadBoards(forceRefresh: Boolean): DomainOutcome<List<BoardDirectoryEntry>> =
                DomainOutcome.Success(emptyList())
        }
    private val sources =
        listOf(
            ForumBoardSource(dvach, "2ch", directory),
            ForumBoardSource(fourChan, "4chan", directory),
        )

    @Test
    fun forumSwipesSelectTheNextAndPreviousRegisteredForumWithoutWrapping() {
        assertEquals(fourChan, adjacentForumSource(dvach, sources, BoardSwipeDirection.NEXT)?.site)
        assertEquals(dvach, adjacentForumSource(fourChan, sources, BoardSwipeDirection.PREVIOUS)?.site)
        assertNull(adjacentForumSource(dvach, sources, BoardSwipeDirection.PREVIOUS))
        assertNull(adjacentForumSource(fourChan, sources, BoardSwipeDirection.NEXT))
    }

    @Test
    fun boardSourceSwipesExcludeArchiveAndDoNotWrap() {
        assertEquals("4chan", adjacentBoardSourceTab("dvach", sources, BoardSwipeDirection.NEXT))
        assertNull(adjacentBoardSourceTab("4chan", sources, BoardSwipeDirection.NEXT))
        assertNull(adjacentBoardSourceTab(ARCHIVE_SOURCE_ID, sources, BoardSwipeDirection.PREVIOUS))
        assertNull(adjacentBoardSourceTab("dvach", sources, BoardSwipeDirection.PREVIOUS))
        assertNull(adjacentBoardSourceTab(ARCHIVE_SOURCE_ID, sources, BoardSwipeDirection.NEXT))
    }

    @Test
    fun targetForumKeepsCurrentBoardCodeOrFallsBackToFirstCategoryBoard() {
        val current = address(dvach, "b")
        val targetBoards =
            listOf(
                entry(fourChan, "g", "Видеоигры"),
                entry(fourChan, "a", "Тематика"),
                entry(fourChan, "b", "Тематика"),
            )

        assertEquals(address(fourChan, "b"), boardForForum(current, fourChan, targetBoards))
        assertEquals(
            address(fourChan, "a"),
            boardForForum(address(dvach, "missing"), fourChan, targetBoards),
        )
    }

    private fun entry(
        site: SiteId,
        code: String,
        category: String,
    ) = BoardDirectoryEntry(
        address = address(site, code),
        title = code,
        category = category,
        postsPerHour = 0,
        postingEnabled = true,
    )

    private fun address(
        site: SiteId,
        code: String,
    ) = BoardAddress(site, requireNotNull(BoardId.parse(code)))
}
