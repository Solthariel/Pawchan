package com.solthariel.pawchan.infrastructure.sites

import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class CachedBoardDirectoryTest {
    private val site = requireNotNull(SiteId.parse("dvach"))
    private val board =
        BoardDirectoryEntry(
            BoardAddress(site, requireNotNull(BoardId.parse("b"))),
            "Бред",
            "Разное",
            9,
            true,
            false,
        )

    @Test fun persistsAndRestoresWithoutNetwork() =
        runTest {
            val dir = Files.createTempDirectory("boards-test").toFile()
            try {
                var calls = 0
                val api =
                    object : BoardDirectoryRepository {
                        override suspend fun loadBoards(forceRefresh: Boolean): BoardResult {
                            calls++
                            return DomainOutcome.Success(listOf(board))
                        }
                    }
                val file = dir.resolve("dvach.bin")
                CachedBoardDirectory(site, api, file).loadBoards(true)
                val fresh = CachedBoardDirectory(site, api, file)
                assertEquals(DomainOutcome.Success(listOf(board)), fresh.loadBoards(false))
                assertEquals(1, calls)
                assertEquals(listOf(board), fresh.snapshots.value)
            } finally {
                dir.deleteRecursively()
            }
        }

    @Test fun failedRefreshKeepsPreviouslyLoadedSnapshot() =
        runTest {
            val dir = Files.createTempDirectory("boards-test").toFile()
            try {
                var fail = false
                val api =
                    object : BoardDirectoryRepository {
                        override suspend fun loadBoards(forceRefresh: Boolean): BoardResult =
                            if (fail) {
                                DomainOutcome.Failure(DomainFailure.NetworkUnavailable)
                            } else {
                                DomainOutcome.Success(listOf(board))
                            }
                    }
                val cache = CachedBoardDirectory(site, api, dir.resolve("boards.bin"))
                cache.loadBoards(true)
                fail = true
                assertEquals(DomainOutcome.Failure(DomainFailure.NetworkUnavailable), cache.loadBoards(true))
                assertEquals(listOf(board), cache.snapshots.value)
                assertEquals(DomainOutcome.Success(listOf(board)), cache.loadBoards(false))
            } finally {
                dir.deleteRecursively()
            }
        }

    @Test fun corruptOrForeignCacheIsNotUsed() =
        runTest {
            val dir = Files.createTempDirectory("boards-test").toFile()
            try {
                val file = dir.resolve("boards.bin")
                val successfulResponse = DomainOutcome.Success(listOf(board))
                val api =
                    object : BoardDirectoryRepository {
                        override suspend fun loadBoards(forceRefresh: Boolean): BoardResult = successfulResponse
                    }
                CachedBoardDirectory(site, api, file).loadBoards(true)
                val other = CachedBoardDirectory(requireNotNull(SiteId.parse("4chan")), api, file)
                other.restore()
                assertNull(other.snapshots.value)
                file.writeText("corrupt")
                val broken = CachedBoardDirectory(site, api, file)
                broken.restore()
                assertNull(broken.snapshots.value)
            } finally {
                dir.deleteRecursively()
            }
        }

    @Test fun startupDoesNotDuplicateSuccessfulOnDemandFetch() =
        runTest {
            val dir = Files.createTempDirectory("boards-test").toFile()
            try {
                var calls = 0
                val api =
                    object : BoardDirectoryRepository {
                        override suspend fun loadBoards(forceRefresh: Boolean): BoardResult {
                            calls++
                            return DomainOutcome.Success(listOf(board))
                        }
                    }
                val cache = CachedBoardDirectory(site, api, dir.resolve("boards.bin"))
                cache.loadBoards(false)
                cache.refreshAtStartup()
                assertEquals(1, calls)
            } finally {
                dir.deleteRecursively()
            }
        }

    @Test fun invalidResponseDoesNotReplaceSnapshot() =
        runTest {
            val dir = Files.createTempDirectory("boards-test").toFile()
            try {
                var empty = false
                val api =
                    object : BoardDirectoryRepository {
                        override suspend fun loadBoards(forceRefresh: Boolean): BoardResult =
                            DomainOutcome.Success(if (empty) emptyList() else listOf(board))
                    }
                val cache = CachedBoardDirectory(site, api, dir.resolve("boards.bin"))
                cache.loadBoards(true)
                empty = true
                assertEquals(DomainOutcome.Failure(DomainFailure.InvalidResponse), cache.loadBoards(true))
                assertEquals(listOf(board), cache.snapshots.value)
            } finally {
                dir.deleteRecursively()
            }
        }

    @Test fun cancellationIsNotConvertedToFailure() =
        runTest {
            val dir = Files.createTempDirectory("boards-test").toFile()
            try {
                val cancellation = CancellationException("cancelled")
                val api =
                    object : BoardDirectoryRepository {
                        override suspend fun loadBoards(forceRefresh: Boolean): BoardResult = throw cancellation
                    }
                val cache = CachedBoardDirectory(site, api, dir.resolve("boards.bin"))
                var cancelled = false
                try {
                    cache.loadBoards(true)
                } catch (_: CancellationException) {
                    cancelled = true
                }
                assertTrue(cancelled)
            } finally {
                dir.deleteRecursively()
            }
        }
}

private typealias BoardResult = DomainOutcome<List<BoardDirectoryEntry>>
