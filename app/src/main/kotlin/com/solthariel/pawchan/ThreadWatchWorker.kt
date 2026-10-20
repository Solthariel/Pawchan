package com.solthariel.pawchan

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.solthariel.pawchan.data.database.RoomStorage
import com.solthariel.pawchan.data.drafts.FileWorkspaceRepository
import com.solthariel.pawchan.data.network.DvachHttpClient
import com.solthariel.pawchan.data.network.DvachThreadApi
import com.solthariel.pawchan.domain.settings.AppSettingsRepository
import com.solthariel.pawchan.domain.thread.ThreadCheckPassRepository
import com.solthariel.pawchan.domain.watch.CheckWatchedThread
import com.solthariel.pawchan.domain.watch.ThreadWatchSubscription
import com.solthariel.pawchan.infrastructure.AndroidCookieJar
import com.solthariel.pawchan.infrastructure.dns.withArhivachDns
import com.solthariel.pawchan.watch.BackgroundWatchRunResult
import com.solthariel.pawchan.watch.BackgroundWifiInterceptor
import com.solthariel.pawchan.watch.ThreadWatchBackgroundRunner
import com.solthariel.pawchan.watch.ThreadWatchEventSink
import com.solthariel.pawchan.watch.ThreadWatchNotifier
import com.solthariel.pawchan.workspace.checkWorkspaceReplies
import kotlinx.coroutines.flow.first
import java.io.File

internal class ThreadWatchWorker(
    appContext: Context,
    parameters: WorkerParameters,
    private val preferences: com.solthariel.pawchan.data.preferences.DataStoreSettingsRepository,
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        val behavior = preferences.appSettings.first().behavior
        val clientSettings = behavior.client
        val automation = clientSettings.automation
        if (!com.solthariel.pawchan.watch
                .backgroundNetworkAllowed(applicationContext, automation.wifiOnly)
        ) {
            return Result.success()
        }
        val storage = RoomStorage.create(applicationContext)
        val client =
            DvachHttpClient
                .create(AndroidCookieJar())
                .newBuilder()
                .addInterceptor(
                    com.solthariel.pawchan.data.network
                        .MirrorRoutingInterceptor(clientSettings.mirror),
                ).apply {
                    if (automation.wifiOnly) addNetworkInterceptor(BackgroundWifiInterceptor(applicationContext))
                }.build()
                .withArhivachDns(behavior.dns.arhivach)
        return try {
            val repository =
                ThreadCheckPassRepository(
                    com.solthariel.pawchan.domain.thread.MissingAwareThreadRepository(
                        DvachThreadApi.create(client),
                        com.solthariel.pawchan.data.drafts.FileThreadDeletionStore(
                            File(applicationContext.filesDir, "missing-threads"),
                        ),
                    ),
                )
            if (com.solthariel.pawchan.watch.runBackgroundChecks(
                    applicationContext,
                    repository,
                    client,
                    storage,
                    automation,
                    preferences.usage,
                )
            ) {
                Result.success()
            } else {
                Result.retry()
            }
        } finally {
            storage.close()
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
            client.cache?.close()
        }
    }
}
