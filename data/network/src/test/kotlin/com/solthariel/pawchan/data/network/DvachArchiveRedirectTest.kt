package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DvachArchiveRedirectTest {
    private val address =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(42)),
        )

    @Test
    fun datedArchiveNotFoundRemainsMissingAfterRedirect() {
        for (status in listOf(404, 410)) {
            val (outcome, paths) = load(status)
            assertEquals(DomainOutcome.Failure(DomainFailure.NotFound), outcome)
            assertEquals(listOf(LIVE_PATH, ARCHIVE_PATH, DATED_PATH), paths)
        }
    }

    @Test
    fun existingDatedArchiveIsReadableAndNotDeleted() {
        val (outcome, paths) = load(200)
        val snapshot = (outcome as DomainOutcome.Success).value
        assertEquals(3, paths.size)
        assertTrue(snapshot.flags.archived)
        assertTrue(snapshot.flags.closed)
    }

    @Test
    fun archiveServerFailureIsNotReportedAsDeletion() {
        val (outcome, _) = load(503)
        assertEquals(DomainOutcome.Failure(DomainFailure.HttpStatus(503)), outcome)
    }

    @Test
    fun archiveTransportFailureIsNotReportedAsDeletion() {
        val (outcome, _) = load(null)
        assertEquals(DomainOutcome.Failure(DomainFailure.NetworkUnavailable), outcome)
    }

    @Test
    fun unsafeRedirectsAreNotRequested() {
        val locations =
            listOf(
                "https://other.example$DATED_PATH",
                "http://2ch.su$DATED_PATH",
                "https://user:password@2ch.su$DATED_PATH",
                "/b/arch/2026-10-20/res/43.json",
                "/b/res/42.json",
                "$DATED_PATH?token=secret",
            )
        for (location in locations) {
            val (outcome, paths) = load(200, location)
            assertEquals(DomainOutcome.Failure(DomainFailure.HttpStatus(302)), outcome)
            assertEquals(listOf(LIVE_PATH, ARCHIVE_PATH), paths)
        }
    }

    @Test
    fun archiveRedirectLoopsAreBounded() {
        val (outcome, paths) = load(302)
        assertEquals(DomainOutcome.Failure(DomainFailure.HttpStatus(302)), outcome)
        assertEquals(5, paths.size)
    }

    @Test
    fun datedRedirectsWorkOnEachSupportedMirror() {
        for (host in listOf("2ch.su", "2ch.org", "2ch.life")) {
            val (outcome, paths) = load(404, host = host)
            assertEquals(DomainOutcome.Failure(DomainFailure.NotFound), outcome)
            assertEquals(listOf(LIVE_PATH, ARCHIVE_PATH, DATED_PATH), paths)
        }
    }

    private fun load(
        archiveStatus: Int?,
        location: String = DATED_PATH,
        host: String = "2ch.su",
    ): Pair<DomainOutcome<com.solthariel.pawchan.domain.model.ThreadSnapshot>, List<String>> =
        runBlocking {
            val paths = mutableListOf<String>()
            val client =
                OkHttpClient
                    .Builder()
                    .followRedirects(false)
                    .addInterceptor(MirrorRoutingInterceptor(host))
                    .addInterceptor { chain ->
                        val request = chain.request()
                        val path = request.url.encodedPath
                        paths += path
                        val status =
                            when (path) {
                                LIVE_PATH -> 404
                                ARCHIVE_PATH -> 302
                                else -> archiveStatus ?: throw IOException("fixture offline")
                            }
                        Response
                            .Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(status)
                            .message("fixture")
                            .apply { if (status == 302) header("Location", location) }
                            .body(ARCHIVE_PAYLOAD.toResponseBody())
                            .build()
                    }.build()
            try {
                DvachThreadApi.create(client).loadThread(address) to paths
            } finally {
                client.dispatcher.executorService.shutdownNow()
                client.connectionPool.evictAll()
            }
        }
}

private const val LIVE_PATH = "/b/res/42.json"
private const val ARCHIVE_PATH = "/b/arch/res/42.json"
private const val DATED_PATH = "/b/arch/2026-10-20/res/42.json"
private const val ARCHIVE_PAYLOAD =
    """{"threads":[{"posts":[{"num":42,"parent":0,"subject":"Тест","comment":"Текст","files":[]}]}]}"""
