package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.data.network.proxy.ScopedProxySelector
import com.solthariel.pawchan.data.network.proxy.viaProxyRouter
import com.solthariel.pawchan.domain.boards.BoardArchiveSnapshot
import com.solthariel.pawchan.domain.boards.BoardDetailsRepository
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.boards.BoardInformation
import com.solthariel.pawchan.domain.catalog.CatalogRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardDescriptor
import com.solthariel.pawchan.domain.model.CatalogSnapshot
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ThreadRepository
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.CookieJar
import okhttp3.OkHttpClient

/** Public read-only 4chan adapter. It owns a cookie-free, non-redirecting client. */
class FourChanReadApi internal constructor(
    private val transport: FourChanTransport,
    private val ownedClient: OkHttpClient? = null,
) : BoardDirectoryRepository,
    CatalogRepository,
    ThreadRepository,
    BoardDetailsRepository,
    AutoCloseable {
    private val decoder =
        FourChanPayloadDecoder(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            },
        )

    @Volatile
    private var boards: List<BoardDirectoryEntry>? = null

    override suspend fun loadBoards(forceRefresh: Boolean): DomainOutcome<List<BoardDirectoryEntry>> {
        if (!forceRefresh) boards?.let { return DomainOutcome.Success(it) }
        return transport.fetch(listOf("boards.json")) { it.decode(decoder::boards) }.also {
            if (it is DomainOutcome.Success) boards = it.value
        }
    }

    override suspend fun loadCatalog(board: BoardAddress): DomainOutcome<CatalogSnapshot> {
        if (!board.isFourChan()) return DomainOutcome.Failure(DomainFailure.UnsupportedSite)
        val info = boards?.firstOrNull { it.address == board }
        val descriptor = BoardDescriptor(board, info?.title ?: "/${board.board.value}/", info?.category == "Безопасные")
        return transport.fetch(listOf(board.board.value, "catalog.json")) { body ->
            body.decode { decoder.catalog(descriptor, it) }
        }
    }

    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> {
        if (!address.board.isFourChan()) return DomainOutcome.Failure(DomainFailure.UnsupportedSite)
        val path = listOf(address.board.board.value, "thread", "${address.thread.value}.json")
        return transport.fetch(path, isThread = true) { body -> body.decode { decoder.thread(address, it) } }
    }

    override suspend fun loadInformation(board: BoardAddress): DomainOutcome<BoardInformation> {
        if (!board.isFourChan()) return DomainOutcome.Failure(DomainFailure.UnsupportedSite)
        return when (val result = loadBoards(false)) {
            is DomainOutcome.Failure -> {
                result
            }

            is DomainOutcome.Success -> {
                val entry = result.value.firstOrNull { it.address == board }
                if (entry == null) {
                    DomainOutcome.Failure(DomainFailure.NotFound)
                } else {
                    DomainOutcome.Success(
                        BoardInformation(board, entry.title, "", null, null, null),
                    )
                }
            }
        }
    }

    override suspend fun loadArchive(
        board: BoardAddress,
        page: Int?,
    ): DomainOutcome<BoardArchiveSnapshot> = DomainOutcome.Failure(DomainFailure.UnsupportedSite)

    override fun close() {
        ownedClient?.dispatcher?.executorService?.shutdown()
        ownedClient?.connectionPool?.evictAll()
    }

    companion object {
        fun create(
            networkInterceptor: okhttp3.Interceptor? = null,
            router: ScopedProxySelector? = null,
        ): FourChanReadApi {
            val client =
                OkHttpClient
                    .Builder()
                    .cookieJar(CookieJar.NO_COOKIES)
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .addInterceptor(ServerBackoffInterceptor())
                    .apply {
                        if (networkInterceptor != null) addNetworkInterceptor(networkInterceptor)
                        if (router != null) viaProxyRouter(router)
                    }.build()
            return FourChanReadApi(FourChanTransport(client), client)
        }
    }
}

private fun BoardAddress.isFourChan(): Boolean = site == FOUR_CHAN_SITE && board.value.matches(FOUR_CHAN_BOARD)

private fun <T> String.decode(parse: (String) -> T?): DomainOutcome<T> =
    try {
        parse(this)?.let { DomainOutcome.Success(it) } ?: DomainOutcome.Failure(DomainFailure.InvalidResponse)
    } catch (_: SerializationException) {
        DomainOutcome.Failure(DomainFailure.InvalidResponse)
    }
