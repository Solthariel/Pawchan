package com.solthariel.pawchan.watch

import android.content.Context
import com.solthariel.pawchan.data.database.RoomStorage
import com.solthariel.pawchan.data.drafts.FileWorkspaceRepository
import com.solthariel.pawchan.domain.settings.AutomationPreferences
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.domain.watch.CheckWatchedThread
import com.solthariel.pawchan.domain.watch.ThreadWatchSubscription
import com.solthariel.pawchan.workspace.checkWorkspaceReplies
import okhttp3.OkHttpClient
import java.io.File

internal suspend fun runBackgroundChecks(
    context: Context,
    repository: ThreadRepository,
    client: OkHttpClient,
    storage: RoomStorage,
    automation: AutomationPreferences,
    statistics: com.solthariel.pawchan.domain.statistics.UsageStatisticsRepository,
): Boolean {
    val runner =
        ThreadWatchBackgroundRunner(
            storage.watches,
            CheckWatchedThread(repository),
            if (automation.notifications.posts) ThreadWatchNotifier(context) else ThreadWatchEventSink { _, _ -> },
        )
    val archives =
        com.solthariel.pawchan.archive
            .checkWorkspaceArchives(context, repository, client)
    val notifier = ThreadWatchNotifier(context, replies = true)
    val replies =
        !automation.replies ||
            checkWorkspaceReplies(
                FileWorkspaceRepository(File(context.filesDir, "workspace")),
                repository,
                statistics,
            ) { address, count ->
                if (automation.notifications.replies) {
                    notifier.onNewPosts(ThreadWatchSubscription(address, null, null, 0L), count)
                }
            }
    val favorites = !automation.favorites || refreshBackgroundFavorites(context, storage.library, repository)
    val watched = !automation.favorites || runner.run() == BackgroundWatchRunResult.SUCCESS
    return archives && replies && favorites && watched
}
