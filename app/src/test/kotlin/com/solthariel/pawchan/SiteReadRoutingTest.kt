package com.solthariel.pawchan

import com.solthariel.pawchan.domain.catalog.CatalogRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.infrastructure.sites.SiteCatalogRouter
import com.solthariel.pawchan.infrastructure.sites.SiteThreadRouter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteReadRoutingTest {
    private val first = address("dvach")
    private val second = address("4chan")
    private val unknown = address("other")

    @Test fun catalogRoutesBySiteAndRejectsUnknownSite() =
        runTest {
            val calls = mutableListOf<BoardAddress>()
            val a =
                CatalogRepository { board ->
                    calls.add(board)
                    DomainOutcome.Failure(DomainFailure.NotFound)
                }
            val b =
                CatalogRepository { board ->
                    calls.add(board)
                    DomainOutcome.Failure(DomainFailure.NetworkTimeout)
                }
            val map = mutableMapOf(first.board.site to a, second.board.site to b)
            val router = SiteCatalogRouter(map)
            map.clear()
            assertEquals(DomainOutcome.Failure(DomainFailure.NotFound), router.loadCatalog(first.board))
            assertEquals(DomainOutcome.Failure(DomainFailure.NetworkTimeout), router.loadCatalog(second.board))
            assertEquals(DomainOutcome.Failure(DomainFailure.UnsupportedSite), router.loadCatalog(unknown.board))
            assertEquals(listOf(first.board, second.board), calls)
            assertEquals(null, router.loadBoardSpeed(unknown.board))
        }

    @Test fun threadsAndDeletionStateRemainIsolated() =
        runTest {
            val calls = mutableListOf<ThreadAddress>()

            fun repo(
                failure: DomainFailure,
                deleted: Boolean,
            ) = object : ThreadRepository {
                override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> {
                    calls.add(address)
                    return DomainOutcome.Failure(failure)
                }

                override suspend fun isThreadDeleted(address: ThreadAddress) = deleted
            }
            val router =
                SiteThreadRouter(
                    mapOf(
                        first.board.site to repo(DomainFailure.NotFound, true),
                        second.board.site to repo(DomainFailure.NetworkTimeout, false),
                    ),
                )
            assertEquals(DomainOutcome.Failure(DomainFailure.NotFound), router.loadThread(first))
            assertEquals(DomainOutcome.Failure(DomainFailure.NetworkTimeout), router.loadThread(second))
            assertEquals(DomainOutcome.Failure(DomainFailure.UnsupportedSite), router.loadThread(unknown))
            assertTrue(router.isThreadDeleted(first))
            assertFalse(router.isThreadDeleted(second))
            assertFalse(router.isThreadDeleted(unknown))
            assertEquals(listOf(first, second), calls)
        }

    @Test fun unsupportedSourceCannotReachAnotherSitesReporter() =
        runTest {
            var calls = 0
            val reporter =
                object : com.solthariel.pawchan.domain.thread.ReportingThreadRepository {
                    override suspend fun loadThread(address: ThreadAddress): DomainOutcome.Failure =
                        DomainOutcome.Failure(DomainFailure.NotFound)

                    override suspend fun reportPost(
                        address: ThreadAddress,
                        post: com.solthariel.pawchan.domain.model.PostId,
                        comment: String,
                    ): com.solthariel.pawchan.domain.posting.PostingOutcome<Unit> {
                        calls++
                        return com.solthariel.pawchan.domain.posting.PostingOutcome
                            .Success(Unit)
                    }
                }
            val router = SiteThreadRouter(mapOf(first.site to reporter))
            assertTrue(router.supportsReporting(first))
            assertFalse(router.supportsReporting(second))
            assertTrue(
                router.reportPost(
                    second,
                    requireNotNull(
                        com.solthariel.pawchan.domain.model.PostId
                            .parse(42),
                    ),
                    "test",
                ) is com.solthariel.pawchan.domain.posting.PostingOutcome.Failure,
            )
            assertEquals(0, calls)
            router.reportPost(
                first,
                requireNotNull(
                    com.solthariel.pawchan.domain.model.PostId
                        .parse(42),
                ),
                "test",
            )
            assertEquals(1, calls)
        }

    private fun address(site: String): ThreadAddress =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse(site)), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(42)),
        )
}
