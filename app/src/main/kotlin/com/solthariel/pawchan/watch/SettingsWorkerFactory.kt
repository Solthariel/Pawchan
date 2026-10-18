package com.solthariel.pawchan.watch

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.solthariel.pawchan.ArchiveMediaWorker
import com.solthariel.pawchan.ThreadWatchWorker
import com.solthariel.pawchan.data.preferences.DataStoreSettingsRepository

/** Application owns DataStore; workers borrow the same instance, never create concurrent stores. */
internal class SettingsWorkerFactory(
    private val settings: DataStoreSettingsRepository,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? =
        when (workerClassName) {
            ThreadWatchWorker::class.java.name -> {
                ThreadWatchWorker(appContext, workerParameters, settings)
            }

            ArchiveMediaWorker::class.java.name -> {
                ArchiveMediaWorker(appContext, workerParameters, settings)
            }

            com.solthariel.pawchan.MediaDownloadWorker::class.java.name -> {
                com.solthariel.pawchan.MediaDownloadWorker(appContext, workerParameters, settings)
            }

            else -> {
                null
            }
        }
}
