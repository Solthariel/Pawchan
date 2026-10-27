package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardSpeedMemoryTest {
    @Test fun knownSpeedIsUsedImmediatelyWithoutAFreshDirectoryRequest() =
        runTest {
            val directory =
                object : BoardDirectoryRepository {
                    override suspend fun loadBoards(forceRefresh: Boolean): DomainOutcome<List<BoardDirectoryEntry>> =
                        error("Catalog must not await the directory")

                    override fun knownBoardSpeed(board: BoardAddress): Int = 1427
                }
            val result =
                DvachCatalogApi.create(client(payload("")), directory).loadCatalog(address()) as DomainOutcome.Success
            assertEquals(1427, result.value.boardSpeed)
            assertTrue(result.value.boardSpeedAvailable)
        }

    @Test fun freshDirectoryReplacesOldObservationAndZeroIsValid() =
        runTest {
            val payload = """{"boards":[{"id":"b","name":"Бред","speed":1427}]}"""
            val directory = DvachBoardDirectoryApi.create(client(payload))
            directory.loadBoards(forceRefresh = true)
            assertEquals(1427, directory.knownBoardSpeed(address()))
            directory.rememberBoardSpeed(address(), 1529)
            assertEquals(1529, directory.knownBoardSpeed(address()))
            directory.loadBoards(forceRefresh = true)
            assertEquals(1427, directory.knownBoardSpeed(address()))
            directory.rememberBoardSpeed(address(), 0)
            assertEquals(0, directory.knownBoardSpeed(address()))
        }

    @Test fun directoryDoesNotOverwriteConcurrentIndexResult() =
        runTest {
            lateinit var directory: DvachBoardDirectoryApi
            val client =
                okhttp3.OkHttpClient
                    .Builder()
                    .addInterceptor { chain ->
                        directory.rememberBoardSpeed(address(), 1529)
                        okhttp3.Response
                            .Builder()
                            .request(chain.request())
                            .protocol(okhttp3.Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body("""{"boards":[{"id":"b","name":"Бред","speed":1427}]}""".toResponseBody())
                            .build()
                    }.build()
            directory = DvachBoardDirectoryApi.create(client)
            directory.rememberBoardSpeed(address(), 1529)
            directory.loadBoards(forceRefresh = true)
            assertEquals(1529, directory.knownBoardSpeed(address()))
        }

    @Test fun failedDirectoryRefreshKeepsLastKnownSpeed() =
        runTest {
            val directory = DvachBoardDirectoryApi.create(client("invalid JSON"))
            directory.rememberBoardSpeed(address(), 0)
            assertTrue(directory.loadBoards(forceRefresh = true) is DomainOutcome.Failure)
            assertEquals(0, directory.knownBoardSpeed(address()))
        }

    private fun client(payload: String): okhttp3.OkHttpClient =
        okhttp3.OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                okhttp3.Response
                    .Builder()
                    .request(chain.request())
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(payload.toResponseBody())
                    .build()
            }.build()

    private fun address(): BoardAddress =
        BoardAddress(
            requireNotNull(SiteId.parse("dvach")),
            requireNotNull(BoardId.parse("b")),
        )

    private fun payload(speed: String): String = """{$speed"board":{"id":"b","name":"Бред"},"threads":[]}"""
}
