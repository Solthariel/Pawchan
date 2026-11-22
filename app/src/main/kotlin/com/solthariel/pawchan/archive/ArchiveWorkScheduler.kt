package com.solthariel.pawchan.archive

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.solthariel.pawchan.ArchiveMediaWorker
import java.util.concurrent.TimeUnit

internal object ArchiveWorkScheduler {
    suspend fun enqueue(context: Context) {
        val request =
            OneTimeWorkRequestBuilder<ArchiveMediaWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, ARCHIVE_RETRY_MINUTES, TimeUnit.MINUTES)
                .build()
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                WorkManager
                    .getInstance(context.applicationContext)
                    .enqueueUniqueWork("archive-media", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
                    .result
                    .get()
                Unit
            } catch (failure: java.util.concurrent.ExecutionException) {
                throw java.io.IOException("Не удалось поставить файлы копии в очередь", failure)
            } catch (failure: InterruptedException) {
                Thread.currentThread().interrupt()
                throw java.io.IOException("Постановка файлов в очередь прервана", failure)
            }
        }
    }
}

private const val ARCHIVE_RETRY_MINUTES = 15L
