package com.solthariel.pawchan

import com.solthariel.pawchan.boards.boardCategories
import com.solthariel.pawchan.boards.filterBoards
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.infrastructure.sites.FourChanBoardDirectory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FourChanCategoriesTest {
    @Test fun groupsKnownBoardsAndKeepsUnknownBoardsVisible() =
        runTest {
            val source =
                listOf(
                    entry("a", "4chan"),
                    entry("v", "4chan"),
                    entry("h", "4chan"),
                    entry("new", "4chan"),
                    entry("newadult", "4chan").copy(category = "Взрослым"),
                )
            val directory = FourChanBoardDirectory(fakeDirectory(DomainOutcome.Success(source)))
            val boards = (directory.loadBoards() as DomainOutcome.Success).value
            val groups =
                boardCategories(boards).associate { category ->
                    category.title to category.boards.map { board -> board.address.board.value }
                }
            assertEquals(listOf("a"), groups["Японская культура"])
            assertEquals(listOf("v"), groups["Видеоигры"])
            assertEquals(listOf("h"), groups["Взрослым (18+)"])
            assertEquals(listOf("new"), groups["Остальное"])
            assertEquals(listOf("newadult"), groups["Остальное (18+)"])
            assertEquals(listOf("v"), filterBoards(boards, "видеоигры").map { it.address.board.value })
        }

    @Test fun otherSourcesAndFailuresRemainUnchanged() =
        runTest {
            val entry = entry("a", "dvach")
            val directory = FourChanBoardDirectory(fakeDirectory(DomainOutcome.Success(listOf(entry))))
            assertEquals(entry, (directory.loadBoards() as DomainOutcome.Success).value.single())
            val failing = FourChanBoardDirectory(fakeDirectory(DomainOutcome.Failure(DomainFailure.NetworkUnavailable)))
            assertTrue(failing.loadBoards() is DomainOutcome.Failure)
        }

    private fun fakeDirectory(result: DomainOutcome<List<BoardDirectoryEntry>>) =
        object : BoardDirectoryRepository {
            override suspend fun loadBoards(forceRefresh: Boolean) = result
        }

    private fun entry(
        code: String,
        site: String,
    ) = BoardDirectoryEntry(
        BoardAddress(requireNotNull(SiteId.parse(site)), requireNotNull(BoardId.parse(code))),
        code,
        "Безопасные",
        0,
        false,
    )
}
