package com.solthariel.pawchan

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.solthariel.pawchan.data.network.DvachHttpClient
import com.solthariel.pawchan.infrastructure.AndroidCookieJar
import com.solthariel.pawchan.media.MediaDownloadResult
import com.solthariel.pawchan.media.MediaDownloader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.io.IOException

private data class MediaDownloadInput(
    val url: String,
    val destination: Uri,
)

internal class MediaDownloadWorker(
    appContext: Context,
    parameters: WorkerParameters,
    private val settings: com.solthariel.pawchan.data.preferences.DataStoreSettingsRepository,
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        val input = readInput() ?: return Result.failure()
        val mirror =
            settings.appSettings
                .first()
                .behavior.client.mirror
        val apiClient =
            DvachHttpClient
                .create(AndroidCookieJar())
                .newBuilder()
                .addInterceptor(
                    com.solthariel.pawchan.data.network
                        .MirrorRoutingInterceptor(mirror),
                ).build()
        val contentClient = DvachHttpClient.createContentClient(apiClient)
        return try {
            download(MediaDownloader(contentClient), input.url, input.destination)
        } finally {
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
    ): Result {
        return try {
            val stream =
                applicationContext.contentResolver.openOutputStream(destination, "w")
                    ?: return Result.failure()
            val result = stream.use { output -> downloader.download(url, output) }
            when (result) {
                MediaDownloadResult.Success -> {
                    recordSavedDownload(applicationContext.contentResolver, destination, url, settings.usage)
                    Result.success()
                }

                MediaDownloadResult.RetryableFailure -> {
                    Result.retry()
                }

                MediaDownloadResult.PermanentFailure -> {
                    Result.failure()
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            Result.retry()
        } catch (_: SecurityException) {
            Result.failure()
        }
    }

    companion object {
        const val URL_KEY = "url"
        const val DESTINATION_KEY = "destination"
    }
}
