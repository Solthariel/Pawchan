package com.solthariel.pawchan

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.solthariel.pawchan.data.drafts.ArchiveMediaStore
import com.solthariel.pawchan.data.drafts.ArchiveMediaUpdater
import com.solthariel.pawchan.data.drafts.FileWorkspaceRepository
import com.solthariel.pawchan.data.network.DvachHttpClient
import com.solthariel.pawchan.domain.settings.AppSettingsRepository
import com.solthariel.pawchan.infrastructure.AndroidCookieJar
import com.solthariel.pawchan.watch.BackgroundWifiInterceptor
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.IOException

/** Unique WorkManager owner of durable media downloads; does not publish threads or reencode files. */
internal class ArchiveMediaWorker(
    context: Context,
    parameters: WorkerParameters,
    private val settings: AppSettingsRepository,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val clientSettings =
            settings.appSettings
                .first()
                .behavior.client
        val wifiOnly = clientSettings.automation.wifiOnly
        if (!com.solthariel.pawchan.watch
                .backgroundNetworkAllowed(applicationContext, wifiOnly)
        ) {
            return Result.retry()
        }
        val client =
            DvachHttpClient
                .create(AndroidCookieJar())
                .newBuilder()
                .addInterceptor(
                    com.solthariel.pawchan.data.network
                        .MirrorRoutingInterceptor(clientSettings.mirror),
                ).apply { if (wifiOnly) addNetworkInterceptor(BackgroundWifiInterceptor(applicationContext)) }
                .followRedirects(false)
                .followSslRedirects(false)
                .build()
        val repository = FileWorkspaceRepository(File(applicationContext.filesDir, "workspace"))
        val media = ArchiveMediaStore(File(applicationContext.filesDir, "archive-media"), client)
        return try {
            val updater = ArchiveMediaUpdater(repository, media)
            var complete = true
            repository
                .load()
                .copies
                .filter { it.archive.includeMedia }
                .forEach { if (!updater.update(it)) complete = false }
            media.prune(
                repository
                    .load()
                    .copies
                    .flatMap { it.archive.files }
                    .map { it.file }
                    .toSet(),
            )
            if (complete) Result.success() else Result.retry()
        } catch (_: IOException) {
            Result.retry()
        } finally {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
    }
}
