package com.solthariel.pawchan.infrastructure

import android.content.Context
import coil3.ImageLoader
import com.solthariel.pawchan.data.network.DvachBoardDetailsApi
import com.solthariel.pawchan.data.network.DvachBoardDirectoryApi
import com.solthariel.pawchan.data.network.DvachCatalogApi
import com.solthariel.pawchan.data.network.DvachHttpClient
import com.solthariel.pawchan.data.network.DvachPostingApi
import com.solthariel.pawchan.data.network.DvachThreadApi
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
    private val repositories: NetworkRepositories,
    val mediaLoaders: MediaLoaders,
    private val cacheDirectory: File,
    val mirror: com.solthariel.pawchan.data.network.MirrorRoutingInterceptor,
) : AutoCloseable {
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
        mediaLoaders.mediaDownloadQueue.close()
        mediaLoaders.thumbnailLoader.close()
        mediaLoaders.vocaroo?.close()
        mediaLoaders.imageLoader.shutdown()
        contentClient.dispatcher.executorService.shutdown()
        httpClient.dispatcher.executorService.shutdown()
        // Закрытие TLS-соединений пишет в сокет: на главном потоке это NetworkOnMainThreadException.
        Thread({
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
        ): AppNetworkStack {
            val cookies = AndroidCookieJar().apply { selectOrigin(mirrorHost) }
            val totalBytes =
                cacheMb
                    .coerceIn(StoragePreferences.MIN_MB, StoragePreferences.MAX_MB)
                    .toLong() * BYTES_PER_MB
            val mirror =
                com.solthariel.pawchan.data.network
                    .MirrorRoutingInterceptor(mirrorHost)
            val httpClient =
                DvachHttpClient
                    .create(cookies)
                    .newBuilder()
                    .addInterceptor(mirror)
                    .build()
            val boardDirectory = DvachBoardDirectoryApi.create(httpClient)
            val contentClient = DvachHttpClient.createContentClient(httpClient)
            val imageLoader =
                createAppImageLoader(
                    context,
                    contentClient,
                    totalBytes - totalBytes / THUMBNAIL_CACHE_SHARE,
                )
            val thumbnailLoader =
                ThumbnailLoader(
                    context.applicationContext,
                    contentClient,
                    totalBytes / THUMBNAIL_CACHE_SHARE,
                )
            val threadApi = DvachThreadApi.create(httpClient)
            return AppNetworkStack(
                httpClient = httpClient,
                mirror = mirror,
                cacheDirectory = context.cacheDir,
                contentClient = contentClient,
                repositories =
                    NetworkRepositories(
                        boardDirectory = boardDirectory,
                        boardDetails = DvachBoardDetailsApi.create(httpClient),
                        postingAccess = cookies,
                        catalog = DvachCatalogApi.create(httpClient, boardDirectory),
                        thread = threadApi,
                        posting = DvachPostingApi.create(httpClient),
                        votes = threadApi,
                    ),
                mediaLoaders =
                    MediaLoaders(
                        imageLoader = imageLoader,
                        thumbnailLoader = thumbnailLoader,
                        mediaDownloadQueue = MediaDownloadQueue(context, settings),
                        contentClient = contentClient,
                        threadRepository = DvachThreadApi.create(httpClient),
                        vocaroo = VocarooNetwork(context.applicationContext, contentClient),
                    ),
            )
        }
    }
}

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
    /** Записи Vocaroo: DoH, Cronet и журнал; null — обычный клиент медиа (тесты, превью). */
    val vocaroo: VocarooNetwork? = null,
) {
    val viewer = MediaViewerResources(imageLoader, contentClient)
}

private const val THUMBNAIL_CACHE_SHARE = 4L
private const val BYTES_PER_MB = 1_000_000L
