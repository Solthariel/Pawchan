package com.solthariel.pawchan.downloads

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import java.util.UUID

/** Observes WorkManager after interruption without assuming every stop is a cancellation. */
internal class DownloadStopMonitor(
    private val context: Context,
    private val history: DownloadHistoryStore,
    private val scope: CoroutineScope,
) {
    private val jobs = mutableMapOf<UUID, Job>()

    @Synchronized
    fun stopped(
        id: UUID,
        name: String,
        attempt: Int,
    ) {
        val notifications = DownloadNotifications(context)
        try {
            notifications.clear(id.toString())
        } catch (failure: SecurityException) {
            android.util.Log.w("MediaDownload", "Cannot clear interrupted notification", failure)
        } catch (failure: IllegalStateException) {
            android.util.Log.w("MediaDownload", "Cannot clear interrupted notification", failure)
        }
        jobs.remove(id)?.cancel()
        val job =
            scope.launch(start = CoroutineStart.LAZY) {
                try {
                    val work =
                        WorkManager
                            .getInstance(context)
                            .getWorkInfoByIdFlow(id)
                            .map { it?.let { info -> DownloadWorkSnapshot(info.state, info.runAttemptCount) } }
                    val observer = coroutineContext[Job]
                    watchStoppedWork(work, attempt) { status -> publish(id, name, status, notifications, observer) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: SQLiteException) {
                    android.util.Log.e("MediaDownload", "Cannot observe stopped download", failure)
                } catch (failure: IllegalStateException) {
                    android.util.Log.e("MediaDownload", "Cannot observe stopped download", failure)
                } finally {
                    removeFinished(id, coroutineContext[Job])
                }
            }
        jobs[id] = job
        job.start()
    }

    @Synchronized
    private fun publish(
        id: UUID,
        name: String,
        status: DownloadStatus,
        notifications: DownloadNotifications,
        observer: Job?,
    ) {
        if (jobs[id] !== observer) return
        try {
            if (history.setStatusIfActive(id.toString(), status)) notifications.update(id.toString(), name, status)
        } catch (failure: SQLiteException) {
            android.util.Log.e("MediaDownload", "Cannot reconcile stopped download", failure)
        } catch (failure: SecurityException) {
            android.util.Log.w("MediaDownload", "Notifications unavailable", failure)
        } catch (failure: IllegalArgumentException) {
            android.util.Log.w("MediaDownload", "Invalid download notification", failure)
        } catch (failure: IllegalStateException) {
            android.util.Log.w("MediaDownload", "Cannot reconcile stopped download", failure)
        }
    }

    @Synchronized
    private fun removeFinished(
        id: UUID,
        job: Job?,
    ) {
        if (jobs[id] === job) jobs.remove(id)
    }
}

internal fun interruptedStatus(state: WorkInfo.State): DownloadStatus =
    when (state) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadStatus.WAITING
        WorkInfo.State.RUNNING -> DownloadStatus.RUNNING
        WorkInfo.State.SUCCEEDED -> DownloadStatus.SUCCEEDED
        WorkInfo.State.FAILED -> DownloadStatus.FAILED
        WorkInfo.State.CANCELLED -> DownloadStatus.CANCELLED
    }

internal data class DownloadWorkSnapshot(
    val state: WorkInfo.State,
    val attempt: Int,
)

internal suspend fun watchStoppedWork(
    work: Flow<DownloadWorkSnapshot?>,
    stoppedAttempt: Int,
    publish: (DownloadStatus) -> Unit,
) {
    work
        .dropWhile { it != null && it.state == WorkInfo.State.RUNNING && it.attempt <= stoppedAttempt }
        .onEach { snapshot -> snapshot?.let { publish(interruptedStatus(it.state)) } }
        .takeWhile { it != null && !it.state.isFinished }
        .collect { }
}
