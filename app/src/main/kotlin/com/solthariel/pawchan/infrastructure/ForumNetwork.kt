package com.solthariel.pawchan.infrastructure

import com.solthariel.pawchan.R
import com.solthariel.pawchan.data.network.DvachBoardDetailsApi
import com.solthariel.pawchan.data.network.DvachBoardDirectoryApi
import com.solthariel.pawchan.data.network.DvachCatalogApi
import com.solthariel.pawchan.data.network.DvachPostingApi
import com.solthariel.pawchan.data.network.DvachThreadApi
import com.solthariel.pawchan.data.network.FourChanReadApi
import com.solthariel.pawchan.data.network.proxy.proxyRouter
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.thread.MissingAwareThreadRepository
import com.solthariel.pawchan.domain.thread.ThreadDeletionStore
import com.solthariel.pawchan.infrastructure.sites.CachedBoardDirectory
import com.solthariel.pawchan.infrastructure.sites.ForumBoardSource
import com.solthariel.pawchan.infrastructure.sites.ForumCapabilities
import com.solthariel.pawchan.infrastructure.sites.FourChanBoardDirectory
import com.solthariel.pawchan.infrastructure.sites.SiteBoardDetailsRouter
import com.solthariel.pawchan.infrastructure.sites.SiteBoardDirectory
import com.solthariel.pawchan.infrastructure.sites.SiteCatalogRouter
import com.solthariel.pawchan.infrastructure.sites.SiteThreadRouter
import okhttp3.OkHttpClient
import java.io.File

internal class ForumNetwork(
    val repositories: NetworkRepositories,
    val sources: List<ForumBoardSource>,
    private val fourChan: FourChanReadApi,
) : AutoCloseable {
    override fun close() = fourChan.close()

    companion object {
        fun create(
            client: OkHttpClient,
            cookies: AndroidCookieJar,
            deleted: ThreadDeletionStore,
            directoryCache: File,
        ): ForumNetwork {
            val primary = requireNotNull(SiteId.parse("dvach"))
            val secondary = requireNotNull(SiteId.parse("4chan"))
            val directory = DvachBoardDirectoryApi.create(client)
            val thread = DvachThreadApi.create(client)
            val fourChan = FourChanReadApi.create(router = client.proxyRouter())
            val sources =
                listOf(
                    ForumBoardSource(
                        primary,
                        "2ch",
                        CachedBoardDirectory(primary, directory, File(directoryCache, "dvach.bin")),
                        ForumCapabilities(
                            replies = true,
                            newThreads = true,
                            boardArchive = true,
                            boardInformation = true,
                            boardSpeed = true,
                            tags = true,
                            archiveBrowser = true,
                        ),
                        R.drawable.forum_2ch,
                    ),
                    ForumBoardSource(
                        secondary,
                        "4chan",
                        CachedBoardDirectory(
                            secondary,
                            FourChanBoardDirectory(fourChan),
                            File(directoryCache, "4chan.bin"),
                        ),
                        ForumCapabilities(boardInformation = true),
                        R.drawable.forum_4chan,
                    ),
                )
            val repositories =
                NetworkRepositories(
                    boardDirectory = SiteBoardDirectory(sources),
                    boardDetails =
                        SiteBoardDetailsRouter(
                            mapOf(primary to DvachBoardDetailsApi.create(client), secondary to fourChan),
                        ),
                    postingAccess = cookies,
                    catalog =
                        SiteCatalogRouter(
                            mapOf(primary to DvachCatalogApi.create(client, directory), secondary to fourChan),
                        ),
                    thread =
                        MissingAwareThreadRepository(
                            SiteThreadRouter(mapOf(primary to thread, secondary to fourChan)),
                            deleted,
                        ),
                    posting = DvachPostingApi.create(client),
                    votes = thread,
                )
            return ForumNetwork(repositories, sources, fourChan)
        }
    }
}
