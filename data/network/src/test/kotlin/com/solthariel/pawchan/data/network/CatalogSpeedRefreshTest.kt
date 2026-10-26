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

class CatalogSpeedRefreshTest {
    private val decoder = CatalogPayloadDecoder(Json { ignoreUnknownKeys = true })

    @Test fun freshZeroIsAvailableAndCannotBeReplacedByAnAlias() {
        val result = requireNotNull(decoder.decode(payload("\"board_speed\":0,\"speed\":999,")))
        assertEquals(0, result.boardSpeed)
        assertTrue(result.boardSpeedAvailable)
    }

    @Test fun absentInvalidAndNegativeSpeedRemainUnknown() {
        for (field in listOf("", "\"board_speed\":-1,", "\"board_speed\":\"bad\",")) {
            assertFalse(requireNotNull(decoder.decode(payload(field))).boardSpeedAvailable)
        }
    }

    @Test fun successiveResponsesKeepTheirOwnServerSpeedIncludingZero() {
        val values =
            listOf(1218, 1204, 0).map { speed ->
                requireNotNull(decoder.decode(payload("\"board_speed\":$speed,"))).boardSpeed
            }
        assertEquals(listOf(1218, 1204, 0), values)
    }

    @Test fun missingSpeedForcesFreshDirectoryRead() =
        runTest {
            val requests = mutableListOf<Boolean>()
            val directory =
                object : BoardDirectoryRepository {
                    override suspend fun loadBoards(forceRefresh: Boolean): DomainOutcome<List<BoardDirectoryEntry>> {
                        requests += forceRefresh
                        return DomainOutcome.Success(listOf(BoardDirectoryEntry(address(), "Бред", "", 1204, true)))
                    }
                }
            val api = DvachCatalogApi.create(client(payload("")), directory)
            val result = api.loadCatalog(address()) as DomainOutcome.Success
            assertEquals(1204, result.value.boardSpeed)
            assertTrue(result.value.boardSpeedAvailable)
            assertEquals(listOf(true), requests)
        }

    @Test fun catalogZeroDoesNotRequestTheDirectory() =
        runTest {
            var calls = 0
            val directory =
                object : BoardDirectoryRepository {
                    override suspend fun loadBoards(forceRefresh: Boolean): DomainOutcome<List<BoardDirectoryEntry>> {
                        calls++
                        return DomainOutcome.Success(emptyList())
                    }
                }
            val result =
                DvachCatalogApi
                    .create(client(payload("\"board_speed\":0,")), directory)
                    .loadCatalog(address()) as DomainOutcome.Success
            assertEquals(0, result.value.boardSpeed)
            assertEquals(0, calls)
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
