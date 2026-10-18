package com.solthariel.pawchan.archive

import android.content.Context
import com.solthariel.pawchan.data.drafts.ArchiveMediaStore
import com.solthariel.pawchan.data.drafts.FileWorkspaceRepository
import com.solthariel.pawchan.data.network.ArhivachApi
import com.solthariel.pawchan.data.network.ArhivachCookieJar
import com.solthariel.pawchan.domain.archive.ArchivePhase
import com.solthariel.pawchan.domain.archive.ArchiveSynchronizer
import com.solthariel.pawchan.domain.thread.ThreadRepository
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException

internal suspend fun checkWorkspaceArchives(
    context: Context,
    live: ThreadRepository,
    client: OkHttpClient,
): Boolean {
    val workspace = FileWorkspaceRepository(File(context.filesDir, "workspace"))
    val external =
        ArhivachApi(
            client
                .newBuilder()
                .cookieJar(ArhivachCookieJar())
                .followRedirects(false)
                .followSslRedirects(false)
                .build(),
        )
    return try {
        val success =
            !ArchiveUpdatePreference.enabled(context) ||
                ArchiveSynchronizer(workspace, live, external, System::currentTimeMillis).run()
        ArchiveWorkScheduler.enqueue(context)
        success
    } catch (_: IOException) {
        false
    }
}
