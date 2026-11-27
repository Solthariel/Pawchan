package com.solthariel.pawchan.infrastructure

import android.content.Context
import coil3.ImageLoader
import com.solthariel.pawchan.data.network.DvachHttpClient
import com.solthariel.pawchan.data.network.proxy.viaProxyRouter
import com.solthariel.pawchan.domain.boards.BoardDetailsRepository
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.boards.PostingAccessRepository
import com.solthariel.pawchan.domain.catalog.CatalogRepository
import com.solthariel.pawchan.domain.posting.PostingRepository
import com.solthariel.pawchan.domain.settings.StoragePreferences
import com.solthariel.pawchan.domain.thread.PostVoteRepository
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.infrastructure.media.MediaDownloadQueue
import com.solthariel.pawchan.infrastructure.vocaroo.VocarooNetwork
import com.solthariel.pawchan.media.MediaViewerResources
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.thread.PostVoteSession
import okhttp3.OkHttpClient
import java.io.File

internal class AppNetworkStack private constructor(
    private val httpClient: OkHttpClient,
    private val contentClient: OkHttpClient,
    private val forum: ForumNetwork,
    val mediaLoaders: MediaLoaders,
    private val cacheDirectory: File,
    val mirror: com.solthariel.pawchan.data.network.MirrorRoutingInterceptor,
    val github: GithubNetwork,
) : AutoCloseable {
    private val repositories: NetworkRepositories get() = forum.repositories
    val sources get() = forum.sources
    var clearCacheOnExit: Boolean = false
    val boardDetailsRepository: BoardDetailsRepository
        get() = repositories.boardDetails
    val boardDirectoryRepository: BoardDirectoryRepository
        get() = repositories.boardDirectory
    val postingAccessRepository: PostingAccessRepository
        get() = repositories.postingAccess
    val catalogRepository: CatalogRepository
        get() = repositories.catalog
    val threadRepository: ThreadRepository
        get() = repositories.thread
    val postingRepository: PostingRepository
        get() = repositories.posting

    /** Оценки постов живут вместе с сетевым стеком приложения. */
    val postVotes: PostVoteSession = PostVoteSession(repositories.votes)

    override fun close() {
        val clearCache = clearCacheOnExit
        CookieRuleStore.shared?.let { WebViewSiteCookies(mirror.host).deleteOnExit(it.current()) }
        mediaLoaders.mediaDownloadQueue.close()
        mediaLoaders.thumbnailLoader.close()
        mediaLoaders.vocaroo?.close()
        mediaLoaders.imageLoader.shutdown()
        contentClient.dispatcher.executorService.shutdown()
        httpClient.dispatcher.executorService.shutdown()
        // Закрытие TLS-соединений пишет в сокет: на главном потоке это NetworkOnMainThreadException.
        Thread({
            forum.close()
            mirror.recovery?.close()
            httpClient.connectionPool.evictAll()
            httpClient.cache?.close()
            if (clearCache && !clearClosedSessionCache(cacheDirectory)) {
                android.util.Log.w("PawchanCache", "Some temporary files could not be removed")
            }
        }, "network-stack-close").start()
    }

    fun selectMirror(host: String) {
        if (mirror.host == host) return
        mirror.host = host
        (repositories.postingAccess as AndroidCookieJar).selectOrigin(host)
    }

    companion object {
        fun create(
            context: Context,
            settings: com.solthariel.pawchan.domain.settings.AppSettingsRepository,
            cacheMb: Int = StoragePreferences.DEFAULT_MB,
            mirrorHost: String = "2ch.su",
            proxySelector: com.solthariel.pawchan.data.network.proxy.ScopedProxySelector =
                com.solthariel.pawchan.data.network.proxy
                    .ScopedProxySelector(),
        ): AppNetworkStack {
            val cookieRules = CookieRuleStore.get(context)
            WebViewSiteCookies(mirrorHost).deleteOnExit(cookieRules.current())
            val cookies = AndroidCookieJar(rules = cookieRules::current).apply { selectOrigin(mirrorHost) }
            val totalBytes =
                cacheMb
                    .coerceIn(StoragePreferences.MIN_MB, StoragePreferences.MAX_MB)
                    .toLong() * BYTES_PER_MB
            proxySelector.registerMirror(mirrorHost)
            val mirror =
                com.solthariel.pawchan.data.network
                    .MirrorRoutingInterceptor(
                        mirrorHost,
                        com.solthariel.pawchan.data.network.MirrorRecovery
                            .create(mirrorHost, proxySelector),
                        proxySelector::registerMirror,
                    )
            val httpClient =
                DvachHttpClient
                    .create(cookies)
                    .newBuilder()
                    .apply { interceptors().add(0, mirror) }
                    .viaProxyRouter(proxySelector)
                    .build()
            val contentClient = DvachHttpClient.createContentClient(httpClient)
            val forum =
                ForumNetwork.create(
                    httpClient,
                    cookies,
                    com.solthariel.pawchan.data.drafts
                        .FileThreadDeletionStore(File(context.filesDir, "missing-threads")),
                    File(context.filesDir, "board-directories"),
                )
            return AppNetworkStack(
                httpClient = httpClient,
                github = GithubNetwork(httpClient),
                mirror = mirror,
                cacheDirectory = context.cacheDir,
                contentClient = contentClient,
                forum = forum,
                mediaLoaders =
                    createMediaLoaders(
                        context,
                        settings,
                        contentClient,
                        totalBytes,
                        forum.repositories.thread,
                    ),
            )
        }
    }
}

/** Binds app-owned media resources to the shared content client. */
private fun createMediaLoaders(
    context: Context,
    settings: com.solthariel.pawchan.domain.settings.AppSettingsRepository,
    contentClient: OkHttpClient,
    totalBytes: Long,
    threadRepository: ThreadRepository,
): MediaLoaders =
    MediaLoaders(
        imageLoader = createAppImageLoader(context, contentClient, totalBytes - totalBytes / THUMBNAIL_CACHE_SHARE),
        thumbnailLoader =
            ThumbnailLoader(
                context.applicationContext,
                contentClient,
                totalBytes / THUMBNAIL_CACHE_SHARE,
            ),
        mediaDownloadQueue = MediaDownloadQueue(context, settings),
        contentClient = contentClient,
        threadRepository = threadRepository,
        vocaroo = VocarooNetwork(context.applicationContext, contentClient),
    )

internal data class NetworkRepositories(
    val boardDirectory: BoardDirectoryRepository,
    val boardDetails: BoardDetailsRepository,
    val postingAccess: PostingAccessRepository,
    val catalog: CatalogRepository,
    val thread: ThreadRepository,
    val posting: PostingRepository,
    val votes: PostVoteRepository,
)

internal data class MediaLoaders(
    val imageLoader: ImageLoader,
    val thumbnailLoader: ThumbnailLoader,
    val mediaDownloadQueue: MediaDownloadQueue,
    val contentClient: OkHttpClient,
    val threadRepository: ThreadRepository? = null,
    /** Записи Vocaroo: Cronet и журнал; null — обычный клиент медиа (тесты, превью). */
    val vocaroo: VocarooNetwork? = null,
) {
    val viewer = MediaViewerResources(imageLoader, contentClient)
}

private const val THUMBNAIL_CACHE_SHARE = 4L
private const val BYTES_PER_MB = 1_000_000L
