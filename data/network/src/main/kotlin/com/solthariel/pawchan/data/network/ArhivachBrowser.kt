package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.data.network.proxy.ScopedProxySelector
import com.solthariel.pawchan.data.network.proxy.viaProxyRouter
import com.solthariel.pawchan.domain.archive.ArchiveBrowser
import com.solthariel.pawchan.domain.archive.ArchiveTag
import com.solthariel.pawchan.domain.archive.ArchiveThread
import com.solthariel.pawchan.domain.archive.ArchiveThreadPage
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.Call
import okhttp3.CookieJar
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** Cookie-free read-only requests to the archive itself; no posting or redirects. */
class ArhivachBrowser internal constructor(
    private val calls: Call.Factory,
    private val ownedClient: OkHttpClient? = null,
    private val decodeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ArchiveBrowser,
    AutoCloseable {
    override suspend fun searchTags(query: String): DomainOutcome<List<ArchiveTag>> {
        if (query.length > MAX_QUERY_LENGTH) return invalidArchiveResponse()
        val url =
            archiveBaseUrl()
                .newBuilder()
                .addPathSegment("ajax")
                .addPathSegment("")
                .addQueryParameter("callback", JSONP_CALLBACK)
                .addQueryParameter("act", "tagcomplete")
                .addQueryParameter("q", query.trim())
                .addQueryParameter("page", "1")
                .build()
        return get(url.toString(), ::decodeArchiveTags)
    }

    override suspend fun loadThreads(
        tags: List<Long>,
        page: Int,
    ): DomainOutcome<ArchiveThreadPage> {
        val invalidPageOrSize = page !in 0..MAX_PAGE || tags.size > MAX_TAGS
        val invalidTags = tags.any { it <= 0 } || tags.distinct().size != tags.size
        if (invalidPageOrSize || invalidTags) {
            return invalidArchiveResponse()
        }
        val offset = page * ARCHIVE_PAGE_SIZE
        val builder =
            archiveBaseUrl()
                .newBuilder()
                .addPathSegment("index")
                .addPathSegment(offset.toString())
                .addPathSegment("")
        if (tags.isNotEmpty()) builder.addQueryParameter("tags", tags.joinToString(","))
        val url = builder.build().toString()
        return get(url) { decodeArchiveThreadList(it, url, offset + ARCHIVE_PAGE_SIZE) }
    }

    override suspend fun loadThread(id: Long): DomainOutcome<ArchiveThread> {
        if (id <= 0) return invalidArchiveResponse()
        val url =
            archiveBaseUrl()
                .newBuilder()
                .addPathSegment("thread")
                .addPathSegment(id.toString())
                .addPathSegment("")
                .build()
        return get(url.toString()) { decodeArchiveBrowserThread(it, url.toString(), id) }
    }

    private suspend fun <T> get(
        url: String,
        parse: (String) -> T?,
    ): DomainOutcome<T> =
        decodeArchiveInBackground(
            decodeDispatcher,
            payload = {
                calls.arhivachPayload(
                    Request
                        .Builder()
                        .url(url)
                        .header("User-Agent", "Pawchan/0.1 (archive reader)")
                        .get()
                        .build(),
                )
            },
            parse = parse,
        )

    override fun close() {
        ownedClient?.dispatcher?.executorService?.shutdown()
        ownedClient?.connectionPool?.evictAll()
    }

    companion object {
        fun create(router: ScopedProxySelector? = null): ArhivachBrowser {
            val client =
                OkHttpClient
                    .Builder()
                    .apply { if (router != null) viaProxyRouter(router) }
                    .cookieJar(CookieJar.NO_COOKIES)
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .build()
            return ArhivachBrowser(client, client)
        }
    }
}

private fun archiveBaseUrl() = "https://arhivach.vc/".toHttpUrl()

private fun <T> invalidArchiveResponse(): DomainOutcome<T> = DomainOutcome.Failure(DomainFailure.InvalidResponse)

internal const val JSONP_CALLBACK = "pawchanTags"
private const val MAX_TAGS = 5
private const val MAX_QUERY_LENGTH = 200
private const val MAX_PAGE = 1_000_000
