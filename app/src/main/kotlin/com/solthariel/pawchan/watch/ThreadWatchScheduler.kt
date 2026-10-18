package com.solthariel.pawchan.watch

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.solthariel.pawchan.ThreadWatchWorker
import com.solthariel.pawchan.domain.settings.AutomationPreferences
import java.util.concurrent.TimeUnit

internal object ThreadWatchScheduler {
    fun schedule(
        context: Context,
        preferences: AutomationPreferences? = null,
    ) {
        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        val request =
            PeriodicWorkRequestBuilder<ThreadWatchWorker>(
                preferences
                    ?.backgroundMinutes
                    ?.toLong()
                    ?.coerceAtLeast(REPEAT_INTERVAL_MINUTES) ?: REPEAT_INTERVAL_MINUTES,
                TimeUnit.MINUTES,
            ).setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_MINUTES,
                    TimeUnit.MINUTES,
                ).build()
        WorkManager
            .getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                if (preferences == null) ExistingPeriodicWorkPolicy.KEEP else ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
    }
}

private const val REPEAT_INTERVAL_MINUTES = 15L
private const val BACKOFF_MINUTES = 15L
private const val UNIQUE_WORK_NAME = "thread-watch"
