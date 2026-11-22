package com.solthariel.pawchan

import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.infrastructure.sites.ForumBoardSource
import com.solthariel.pawchan.infrastructure.sites.SiteBoardDirectory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SiteDirectoryRoutingTest {
    private val first = BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("g")))
    private val second = first.copy(site = requireNotNull(SiteId.parse("4chan")))

    @Test fun sameBoardCodesKeepSeparateSpeedCachesAndEntries() =
        runTest {
            val left = repository(first, 10)
            val right = repository(second, 20)
            val router =
                SiteBoardDirectory(
                    listOf(ForumBoardSource(first.site, "2ch", left), ForumBoardSource(second.site, "4chan", right)),
                )
            assertEquals(mapOf(first to 10, second to 20), router.boardSpeeds.first())
            assertEquals(10, router.knownBoardSpeed(first))
            assertEquals(20, router.knownBoardSpeed(second))
            assertNull(router.knownBoardSpeed(first.copy(site = requireNotNull(SiteId.parse("unknown")))))
            val result = router.loadBoards(false) as DomainOutcome.Success
            assertEquals(listOf(first, second), result.value.map { it.address })
        }

    @Test fun sourceFailuresAreNotPresentedAsAnEmptySuccessfulDirectory() =
        runTest {
            val failing =
                object : BoardDirectoryRepository {
                    override suspend fun loadBoards(forceRefresh: Boolean): DomainOutcome.Failure =
                        DomainOutcome.Failure(DomainFailure.NetworkTimeout)
                }
            val router =
                SiteBoardDirectory(
                    listOf(
                        ForumBoardSource(first.site, "2ch", repository(first, 10)),
                        ForumBoardSource(second.site, "4chan", failing),
                    ),
                )
            assertEquals(DomainOutcome.Failure(DomainFailure.NetworkTimeout), router.loadBoards(true))
        }

    private fun repository(
        board: BoardAddress,
        speed: Int,
    ) = object : BoardDirectoryRepository {
        override val boardSpeeds = flowOf(mapOf(board to speed))

        override suspend fun loadBoards(forceRefresh: Boolean) =
            DomainOutcome.Success(listOf(BoardDirectoryEntry(board, "Board", "", speed, false)))

        override fun knownBoardSpeed(board: BoardAddress): Int? =
            speed.takeIf {
                board == this@SiteDirectoryRoutingTest.first ||
                    board == second
            }
    }
}
