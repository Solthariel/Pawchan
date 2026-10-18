package com.solthariel.pawchan.threads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.thread.ThreadHistoryRepository
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.domain.thread.mergeThreadHistory
import com.solthariel.pawchan.domain.workspace.withSeen
import com.solthariel.pawchan.workspace.LocalWorkspace
import com.solthariel.pawchan.workspace.WorkspaceController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Счётчики избранных тредов как в Dashchan: пока экран открыт, треды перепроверяются раз в
 * [FAVORITES_REFRESH_MS]; свежий ответ дописывается в историю треда, новые посты считаются от последнего
 * захода. Тред без отметки захода (добавлен из каталога) начинает с нуля.
 */
internal class FavoriteThreadsWatch(
    private val scope: CoroutineScope,
    private val history: ThreadHistoryRepository?,
    private val threads: ThreadRepository?,
    private val workspace: WorkspaceController?,
) {
    var counters by mutableStateOf<Map<ThreadAddress, ThreadCounter>>(emptyMap())
        private set
    private var job: Job? = null

    val refreshing: Boolean
        get() = job?.isActive == true

    /** Счётчики по уже сохранённой истории, без сети. */
    suspend fun load(addresses: List<ThreadAddress>) {
        val history = history ?: return
        val seen = seen()
        counters =
            addresses.associateWith { address ->
                val previous = counters[address] ?: ThreadCounter()
                previous.copy(count = newPostsCount(history.load(address), seen[address]))
            }
    }

    fun refresh(addresses: List<ThreadAddress>) {
        val history = history
        val threads = threads
        if (history == null || threads == null || job?.isActive == true) return
        job =
            scope.launch {
                addresses.forEach { address ->
                    update(address) { it.copy(running = true) }
                    val loaded = threads.loadThread(address)
                    val counter =
                        when (loaded) {
                            is DomainOutcome.Success -> {
                                val merged = mergeThreadHistory(history.load(address), loaded.value)
                                history.save(merged)
                                val last = merged.posts.maxOfOrNull { it.address.post.value }
                                if (last != null && seen()[address] == null) {
                                    workspace?.change { it.withSeen(address, last) }
                                }
                                ThreadCounter(newPostsCount(merged, seen()[address] ?: last))
                            }

                            is DomainOutcome.Failure -> {
                                val missing = loaded.reason == DomainFailure.NotFound
                                (counters[address] ?: ThreadCounter()).copy(deleted = missing, error = !missing)
                            }
                        }
                    update(address) { counter }
                }
            }
    }

    private fun seen(): Map<ThreadAddress, Long> =
        workspace
            ?.state
            ?.value
            ?.marks
            ?.seen
            .orEmpty()

    private fun update(
        address: ThreadAddress,
        transform: (ThreadCounter) -> ThreadCounter,
    ) {
        counters = counters + (address to transform(counters[address] ?: ThreadCounter()))
    }
}

@Composable
internal fun rememberFavoriteThreadsWatch(
    history: ThreadHistoryRepository?,
    threads: ThreadRepository?,
    workspace: WorkspaceController?,
    addresses: List<ThreadAddress>,
    seen: Map<ThreadAddress, Long>,
): FavoriteThreadsWatch {
    val scope = rememberCoroutineScope()
    val watch = remember(history, threads, workspace) { FavoriteThreadsWatch(scope, history, threads, workspace) }
    LaunchedEffect(watch, addresses, seen) { watch.load(addresses) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val binding = LocalWorkspace.current
    val settings =
        binding
            ?.appSettings
            ?.appSettings
            ?.collectAsStateWithLifecycle(null)
            ?.value
    val automation = settings?.behavior?.client?.automation
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(watch, addresses, lifecycle, automation) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                if (automation?.favorites != false &&
                    com.solthariel.pawchan.watch
                        .backgroundNetworkAllowed(context, automation?.wifiOnly == true)
                ) {
                    watch.refresh(addresses)
                }
                delay((automation?.openIntervalSeconds?.toLong()?.times(MILLIS_PER_SECOND)) ?: FAVORITES_REFRESH_MS)
            }
        }
    }
    return watch
}

private const val FAVORITES_REFRESH_MS = 30_000L

private const val MILLIS_PER_SECOND = 1000L
