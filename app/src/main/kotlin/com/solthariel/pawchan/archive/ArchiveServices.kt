package com.solthariel.pawchan.archive

import com.solthariel.pawchan.data.drafts.ArchiveMediaStore
import com.solthariel.pawchan.data.drafts.ArchiveZipWriter
import com.solthariel.pawchan.data.network.ArhivachApi
import com.solthariel.pawchan.data.network.ArhivachCookieJar
import com.solthariel.pawchan.domain.archive.ArchiveSynchronizer
import com.solthariel.pawchan.domain.workspace.WorkspaceRepository
import com.solthariel.pawchan.infrastructure.MediaLoaders
import java.io.File

internal class ArchiveServices(
    directory: File,
    repository: WorkspaceRepository,
    loaders: MediaLoaders,
) {
    val external =
        ArhivachApi(
            loaders.contentClient
                .newBuilder()
                .cookieJar(ArhivachCookieJar())
                .followRedirects(false)
                .followSslRedirects(false)
                .build(),
        )
    val synchronizer =
        ArchiveSynchronizer(repository, requireNotNull(loaders.threadRepository), external, System::currentTimeMillis)
    val media =
        ArchiveMediaStore(
            File(directory, "archive-media"),
            loaders.contentClient
                .newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build(),
        )
    val zip = ArchiveZipWriter(media)
}
