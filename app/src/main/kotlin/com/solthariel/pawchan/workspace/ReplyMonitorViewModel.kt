package com.solthariel.pawchan.workspace

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.archive.ArchiveController
import com.solthariel.pawchan.domain.settings.AppSettingsRepository
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.domain.watch.ThreadWatchSubscription
import com.solthariel.pawchan.watch.ThreadWatchNotifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class ReplyMonitorViewModel(
    application: Application,
    private val repository: ThreadRepository?,
    private val workspace: WorkspaceController,
    private val settings: AppSettingsRepository? = null,
    private val archives: ArchiveController? = null,
) : AndroidViewModel(application) {
    private var job: Job? = null

    fun setActive(active: Boolean) {
        if (!active) {
            job?.cancel()
            job = null
        } else if (job?.isActive != true) {
            job =
                viewModelScope.launch {
                    var nextReplies = 0L
                    while (isActive) {
                        val now = System.currentTimeMillis()
                        val automation =
                            settings
                                ?.appSettings
                                ?.first()
                                ?.behavior
                                ?.client
                                ?.automation
                        val allowed =
                            com.solthariel.pawchan.watch
                                .backgroundNetworkAllowed(getApplication(), automation?.wifiOnly == true)
                        if (now >= nextReplies && allowed) {
                            if (automation?.replies != false) checkReplies(automation?.notifications?.replies == true)
                            nextReplies = now + REPLY_CHECK_INTERVAL_MILLIS
                        }
                        if (allowed) archives?.synchronize()
                        workspace.reload()
                        delay(ARCHIVE_CHECK_INTERVAL_MILLIS)
                    }
                }
        }
    }

    private suspend fun checkReplies(notify: Boolean) {
        val source = repository ?: return
        val notifier = ThreadWatchNotifier(getApplication(), replies = true)
        checkWorkspaceReplies(workspace.repository, source, workspace.statistics) { address, count ->
            if (notify) {
                notifier.onNewPosts(
                    ThreadWatchSubscription(address, null, null, 0L),
                    count,
                )
            }
        }
    }

    companion object {
        fun factory(
            context: Context,
            binding: WorkspaceBinding,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    ReplyMonitorViewModel(
                        context.applicationContext as Application,
                        binding.loaders.threadRepository,
                        binding.controller,
                        binding.appSettings,
                        binding.archives,
                    )
                }
            }
    }
}

private const val REPLY_CHECK_INTERVAL_MILLIS = 60_000L

private const val ARCHIVE_CHECK_INTERVAL_MILLIS = 15_000L
