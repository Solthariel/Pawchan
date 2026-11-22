package com.solthariel.pawchan.infrastructure.media

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.net.toUri
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
import com.solthariel.pawchan.downloads.DownloadOrigin
import com.solthariel.pawchan.downloads.DownloadRecord
import com.solthariel.pawchan.downloads.DownloadStatus
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentOrigin
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaSaveQueue
import com.solthariel.pawchan.navigation.AppRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ExecutionException
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

    override fun useDirectory(directory: Uri): AutoCloseable {
        val lease = permissions?.useDirectory(directory.toString())
        return lease ?: AutoCloseable {}
    }

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
        var committed = false
        try {
            enqueueRecorded(attachment, destination)
            committed = true
        } finally {
            if (committed) lease?.let { permissions?.closeAfterCommit(it) } else lease?.close()
        }
    }

    private fun enqueueRecorded(
        attachment: MediaAttachmentUi,
        destination: Uri,
    ) {
        val request = createMediaDownloadRequest(attachment, destination, suggestedName(attachment))
        val history = (appContext as? PawchanApplication)?.downloadHistory
        history?.insert(
            DownloadRecord(
                request.id.toString(),
                attachment.originalUrl,
                destination.toString(),
                suggestedName(attachment),
                DownloadOrigin(
                    attachment.source.origin
                        ?.threadAddress
                        ?.let(AppRoute::thread),
                    attachment.source.origin?.archiveThreadId,
                    attachment.source.ownerPostId?.takeIf { it > 0 },
                ),
                System.currentTimeMillis(),
                DownloadStatus.QUEUED,
            ),
        )
        var committed = false
        var uncertain = false
        try {
            workManager.enqueue(request).result.get()
            committed = true
        } catch (failure: InterruptedException) {
            uncertain = true
            Thread.currentThread().interrupt()
            throw failure
        } finally {
            if (!committed && !uncertain) history?.remove(request.id.toString())
        }
    }

    fun retry(record: DownloadRecord): Boolean {
        if (record.status != DownloadStatus.FAILED && record.status != DownloadStatus.CANCELLED) return false
        return try {
            (appContext as? PawchanApplication)?.downloadHistory?.setStatus(record.id, record.status)
            val source =
                MediaAttachmentSource(
                    id = record.url,
                    fileName = record.name,
                    originalUrl = record.url,
                    thumbnailUrl = null,
                    ownerPostId = record.origin.postId,
                    origin =
                        MediaAttachmentOrigin(
                            threadAddress = record.origin.threadRoute?.let(AppRoute::threadAddress),
                            archiveThreadId = record.origin.archiveId,
                        ),
                )
            enqueue(MediaAttachmentUi(source, null, AttachmentViewer.NONE), record.destination.toUri())
        } catch (_: android.database.sqlite.SQLiteException) {
            false
        }
    }

    override fun enqueueInFolder(
        attachment: MediaAttachmentUi,
        tree: Uri,
    ): Boolean = enqueue(attachment, tree.buildUpon().fragment("pawchan-pending-${UUID.randomUUID()}").build())

    override fun enqueue(
        attachment: MediaAttachmentUi,
        destination: Uri,
    ): Boolean =
        try {
            val grant = destination.buildUpon().fragment(null).build()
            try {
                appContext.contentResolver.takePersistableUriPermission(
                    grant,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (failure: SecurityException) {
                if (appContext.checkUriPermission(
                        grant,
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
        } catch (_: ExecutionException) {
            false
        } catch (_: android.database.sqlite.SQLiteException) {
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
    displayName: String = attachment.fileName,
): Data =
    Data
        .Builder()
        .putString(MediaDownloadWorker.URL_KEY, attachment.originalUrl)
        .putString(MediaDownloadWorker.DESTINATION_KEY, destination.toString())
        .putString(MediaDownloadWorker.NAME_KEY, displayName)
        .build()

private fun createMediaDownloadRequest(
    attachment: MediaAttachmentUi,
    destination: Uri,
    displayName: String,
): OneTimeWorkRequest =
    OneTimeWorkRequestBuilder<MediaDownloadWorker>()
        .addTag("neko-media")
        .addTag("media-name:${attachment.fileName}")
        .addTag("media-destination:$destination")
        .setInputData(createMediaDownloadInput(attachment, destination, displayName))
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
