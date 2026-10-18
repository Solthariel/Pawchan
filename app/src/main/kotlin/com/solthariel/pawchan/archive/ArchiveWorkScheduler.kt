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
    fun enqueue(context: Context) {
        val request =
            OneTimeWorkRequestBuilder<ArchiveMediaWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, ARCHIVE_RETRY_MINUTES, TimeUnit.MINUTES)
                .build()
        WorkManager
            .getInstance(
                context.applicationContext,
            ).enqueueUniqueWork("archive-media", ExistingWorkPolicy.KEEP, request)
    }
}

private const val ARCHIVE_RETRY_MINUTES = 15L
