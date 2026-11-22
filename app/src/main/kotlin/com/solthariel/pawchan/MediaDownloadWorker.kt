package com.solthariel.pawchan

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.solthariel.pawchan.data.network.DvachHttpClient
import com.solthariel.pawchan.data.network.proxy.viaProxyRouter
import com.solthariel.pawchan.domain.proxy.ProxyService
import com.solthariel.pawchan.downloads.DownloadNotifications
import com.solthariel.pawchan.downloads.DownloadStatus
import com.solthariel.pawchan.media.MediaDownloadResult
import com.solthariel.pawchan.media.MediaDownloader
import com.solthariel.pawchan.media.copyCancellableBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException

private data class MediaDownloadInput(
    val url: String,
    val destination: Uri,
)

internal enum class MediaDownloadAttempt(
    val status: DownloadStatus,
) {
    SUCCESS(DownloadStatus.SUCCEEDED),
    RETRY(DownloadStatus.WAITING),
    FAILURE(DownloadStatus.FAILED),

    ;

    fun workResult(): androidx.work.ListenableWorker.Result =
        when (this) {
            SUCCESS -> {
                androidx.work.ListenableWorker.Result
                    .success()
            }

            RETRY -> {
                androidx.work.ListenableWorker.Result
                    .retry()
            }

            FAILURE -> {
                androidx.work.ListenableWorker.Result
                    .failure()
            }
        }
}

internal class MediaDownloadWorker(
    appContext: Context,
    parameters: WorkerParameters,
    private val settings: com.solthariel.pawchan.data.preferences.DataStoreSettingsRepository,
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result =
        try {
            performWork()
        } catch (cancelled: CancellationException) {
            val name = inputData.getString(NAME_KEY) ?: "Медиа"
            (applicationContext as? PawchanApplication)?.downloadStopMonitor?.stopped(id, name, runAttemptCount)
            throw cancelled
        }

    private suspend fun performWork(): Result {
        updateStatus(DownloadStatus.RUNNING)
        val result =
            try {
                readInput()?.let { input -> runDownload(input) } ?: MediaDownloadAttempt.FAILURE
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: IllegalStateException) {
                android.util.Log.e("MediaDownload", "Download failed", failure)
                MediaDownloadAttempt.FAILURE
            } catch (failure: IllegalArgumentException) {
                android.util.Log.e("MediaDownload", "Invalid download", failure)
                MediaDownloadAttempt.FAILURE
            }
        updateStatus(result.status)
        return result.workResult()
    }

    private suspend fun updateStatus(status: DownloadStatus) =
        withContext(Dispatchers.IO) {
            val name = inputData.getString(NAME_KEY) ?: "Медиа"
            try {
                (applicationContext as? PawchanApplication)?.downloadHistory?.setStatus(id.toString(), status)
            } catch (failure: android.database.sqlite.SQLiteException) {
                android.util.Log.e("MediaDownload", "Cannot update download history", failure)
            }
            try {
                DownloadNotifications(applicationContext).update(id.toString(), name, status)
            } catch (failure: SecurityException) {
                android.util.Log.w("MediaDownload", "Notifications unavailable", failure)
            } catch (failure: IllegalArgumentException) {
                android.util.Log.w("MediaDownload", "Invalid notification", failure)
            } catch (failure: IllegalStateException) {
                android.util.Log.w("MediaDownload", "Notifications unavailable", failure)
            }
        }

    private suspend fun runDownload(input: MediaDownloadInput): MediaDownloadAttempt {
        val appSettings = settings.appSettings.first()
        val mirror = appSettings.behavior.client.mirror
        val router =
            try {
                workerProxyRoutes(settings, mirror, ProxyService.entries.toSet())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                return MediaDownloadAttempt.RETRY
            }
        val apiClient =
            DvachHttpClient
                .create(mediaDownloadCookieJar(mirror))
                .newBuilder()
                .viaProxyRouter(router)
                .addInterceptor(
                    com.solthariel.pawchan.data.network
                        .MirrorRoutingInterceptor(mirror, onRouteHost = router::registerMirror),
                ).build()
        val contentClient = DvachHttpClient.createContentClient(apiClient)
        return try {
            download(MediaDownloader(contentClient), input.url, input.destination)
        } finally {
            contentClient.dispatcher.executorService.shutdown()
            apiClient.dispatcher.executorService.shutdown()
            apiClient.connectionPool.evictAll()
            apiClient.cache?.close()
        }
    }

    private fun readInput(): MediaDownloadInput? =
        inputData.getString(URL_KEY)?.let { url ->
            inputData.getString(DESTINATION_KEY)?.let { destination ->
                MediaDownloadInput(url, destination.toUri())
            }
        }

    private suspend fun download(
        downloader: MediaDownloader,
        url: String,
        destination: Uri,
    ): MediaDownloadAttempt =
        try {
            val result =
                withContext(Dispatchers.IO) {
                    val staging = java.io.File(applicationContext.cacheDir, "pw-download-$id.part")
                    try {
                        val transfer = staging.outputStream().use { downloader.download(url, it) }
                        if (transfer == MediaDownloadResult.Success) {
                            publishStagedDownload(staging, destination, inputData.getString(NAME_KEY) ?: "Медиа", url)
                        }
                        transfer
                    } finally {
                        staging.delete()
                    }
                }
            when (result) {
                MediaDownloadResult.Success -> {
                    MediaDownloadAttempt.SUCCESS
                }

                MediaDownloadResult.RetryableFailure -> {
                    MediaDownloadAttempt.RETRY
                }

                MediaDownloadResult.PermanentFailure -> {
                    MediaDownloadAttempt.FAILURE
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            MediaDownloadAttempt.RETRY
        } catch (_: SecurityException) {
            MediaDownloadAttempt.FAILURE
        }

    private suspend fun publishStagedDownload(
        staging: java.io.File,
        ticket: Uri,
        name: String,
        url: String,
    ) {
        val resolver = applicationContext.contentResolver
        val fresh = ticket.fragment?.startsWith("pawchan-pending-") == true
        val target =
            if (fresh) {
                val tree = ticket.buildUpon().fragment(null).build()
                val parent =
                    DocumentsContract.buildDocumentUriUsingTree(
                        tree,
                        DocumentsContract.getTreeDocumentId(tree),
                    )
                val mime =
                    MimeTypeMap
                        .getSingleton()
                        .getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase())
                DocumentsContract.createDocument(resolver, parent, mime ?: "application/octet-stream", name)
                    ?: throw IOException("Cannot create download document")
            } else {
                ticket // A queued item from an older build: never delete a pre-existing URI.
            }
        var committed = false
        // If cleanup also fails, use() keeps the write failure or cancellation as the primary exception.
        val rollback =
            AutoCloseable {
                if (fresh && !committed && !DocumentsContract.deleteDocument(resolver, target)) {
                    android.util.Log.w("MediaDownload", "Provider refused incomplete document cleanup")
                }
            }
        rollback.use {
            val mode = if (fresh) "w" else "wt"
            val out = resolver.openOutputStream(target, mode) ?: throw IOException("Cannot open download document")
            out.use { output ->
                staging.inputStream().use { input ->
                    copyCancellableBytes(input, output)
                }
            }
            val history = (applicationContext as? PawchanApplication)?.downloadHistory
            history?.setDestination(id.toString(), target.toString())
            committed = true
        }
        try {
            recordSavedDownload(resolver, target, url, settings.usage)
        } catch (failure: IOException) {
            android.util.Log.w("MediaDownload", "Cannot record download statistics", failure)
        } catch (failure: SecurityException) {
            android.util.Log.w("MediaDownload", "Cannot record download statistics", failure)
        }
    }

    companion object {
        const val URL_KEY = "url"
        const val DESTINATION_KEY = "destination"
        const val NAME_KEY = "name"
    }
}
