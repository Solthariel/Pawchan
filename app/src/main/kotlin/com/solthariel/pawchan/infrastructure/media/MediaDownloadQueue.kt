package com.solthariel.pawchan.infrastructure.media

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.solthariel.pawchan.MediaDownloadWorker
import com.solthariel.pawchan.PawchanApplication
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.settings.AppSettingsRepository
import com.solthariel.pawchan.domain.settings.DownloadPreferences
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaSaveQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

internal class MediaDownloadQueue(
    context: Context,
    private val settings: AppSettingsRepository,
) : MediaSaveQueue,
    AutoCloseable {
    private val appContext = context.applicationContext
    private val permissions = (appContext as? PawchanApplication)?.downloadPermissions
    private val workManager by lazy { WorkManager.getInstance(appContext) }

    override fun close() = Unit

    @Volatile var preferences =
        DownloadPreferences()
    override val defaultDirectory: String? get() = preferences.treeUri

    override fun useDirectory(directory: Uri): AutoCloseable = permissions?.useDirectory(directory.toString()) ?: AutoCloseable {}

    override fun suggestedName(attachment: MediaAttachmentUi): String = downloadName(attachment, preferences.naming)

    override suspend fun selectDirectory(uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                appContext.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                val result =
                    settings.updateAppSettings { current ->
                        val client = current.behavior.client
                        current.copy(
                            behavior =
                                current.behavior.copy(
                                    client =
                                        client.copy(
                                            downloads = client.downloads.copy(treeUri = uri.toString()),
                                        ),
                                ),
                        )
                    }
                if (result is DomainOutcome.Success) {
                    preferences = preferences.copy(treeUri = uri.toString())
                    true
                } else {
                    false
                }
            } catch (_: SecurityException) {
                false
            } catch (_: IllegalArgumentException) {
                false
            }
        }

    private fun enqueueWithPermissionLease(
        attachment: MediaAttachmentUi,
        destination: Uri,
    ) {
        val tree =
            if (android.provider.DocumentsContract.isTreeUri(destination)) {
                android.provider.DocumentsContract.buildTreeDocumentUri(
                    destination.authority,
                    android.provider.DocumentsContract.getTreeDocumentId(destination),
                )
            } else {
                null
            }
        val lease = tree?.let { permissions?.useDirectory(it.toString()) }
        try {
            val operation = workManager.enqueue(createMediaDownloadRequest(attachment, destination))
            operation.result.addListener(
                { lease?.let { permissions?.closeAfterCommit(it) } },
                java.util.concurrent.Executor { it.run() },
            )
        } catch (failure: IllegalStateException) {
            lease?.close()
            throw failure
        } catch (failure: IllegalArgumentException) {
            lease?.close()
            throw failure
        }
    }

    override fun enqueue(
        attachment: MediaAttachmentUi,
        destination: Uri,
    ): Boolean =
        try {
            try {
                appContext.contentResolver.takePersistableUriPermission(
                    destination,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (failure: SecurityException) {
                if (appContext.checkUriPermission(
                        destination,
                        android.os.Process.myPid(),
                        android.os.Process.myUid(),
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    throw failure
                }
            }
            enqueueWithPermissionLease(attachment, destination)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        } catch (_: IllegalStateException) {
            false
        }
}

internal fun createMediaDownloadInput(
    attachment: MediaAttachmentUi,
    destination: Uri,
): Data =
    Data
        .Builder()
        .putString(MediaDownloadWorker.URL_KEY, attachment.originalUrl)
        .putString(MediaDownloadWorker.DESTINATION_KEY, destination.toString())
        .build()

private fun createMediaDownloadRequest(
    attachment: MediaAttachmentUi,
    destination: Uri,
): OneTimeWorkRequest =
    OneTimeWorkRequestBuilder<MediaDownloadWorker>()
        .addTag("neko-media")
        .addTag("media-name:${attachment.fileName}")
        .addTag("media-destination:$destination")
        .setInputData(createMediaDownloadInput(attachment, destination))
        .setConstraints(
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build(),
        ).setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            BACKOFF_SECONDS,
            TimeUnit.SECONDS,
        ).build()

private const val BACKOFF_SECONDS = 30L
