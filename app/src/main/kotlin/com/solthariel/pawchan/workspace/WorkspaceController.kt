package com.solthariel.pawchan.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.settings.ReadingPreferences
import com.solthariel.pawchan.domain.thread.ThreadHistoryRepository
import com.solthariel.pawchan.domain.workspace.OpenThread
import com.solthariel.pawchan.domain.workspace.WorkspaceRepository
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import com.solthariel.pawchan.domain.workspace.observe
import com.solthariel.pawchan.domain.workspace.storePost
import com.solthariel.pawchan.domain.workspace.visit
import com.solthariel.pawchan.domain.workspace.withReadingPosition
import com.solthariel.pawchan.navigation.AppRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/** Application-lifecycle owner; persistent mutations are serialized and never replace damaged data. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class WorkspaceController(
    private val scope: CoroutineScope,
    val repository: WorkspaceRepository,
    /** История просмотренных тредов (удалённые посты); null — не сохраняется. */
    val history: ThreadHistoryRepository? = null,
    val statistics: com.solthariel.pawchan.domain.statistics.UsageStatisticsRepository? = null,
) {
    var readingPreferences =
        ReadingPreferences()
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(WorkspaceState())
    private val mutableReady = MutableStateFlow(false)
    private val mutableError = MutableStateFlow<String?>(null)
    val state = mutableState.asStateFlow()
    val pendingReply = MutableStateFlow<PendingPreviewReply?>(null)
    val display =
        state
            .map { Triple(it.posts, it.replies, it.media) }
            .distinctUntilChanged()
            .mapLatest { (posts, replies, media) ->
                withContext(Dispatchers.Default) {
                    prepareWorkspace(WorkspaceState(posts = posts, replies = replies, media = media))
                }
            }.stateIn(scope, SharingStarted.Eagerly, WorkspacePrepared())
    val ready = mutableReady.asStateFlow()
    val error = mutableError.asStateFlow()
    val reading = WorkspaceReadingMemory()
    var latestSnapshot: ThreadSnapshot? = null
        private set
    var offlineAddress by mutableStateOf<ThreadAddress?>(null)
    var requestedPost: RequestedThreadPost? = null

    init {
        reload()
    }

    fun recordUsage(
        metric: com.solthariel.pawchan.domain.statistics.UsageMetric,
        amount: Long = 1L,
        identity: String,
    ) {
        scope.launch { statistics?.record(metric, amount, identity) }
    }

    fun reload() {
        scope.launch {
            mutex.withLock {
                try {
                    mutableState.value = repository.load()
                } catch (failure: IOException) {
                    mutableError.value = failure.message
                } finally {
                    mutableReady.value = true
                }
            }
        }
    }

    fun change(transform: (WorkspaceState) -> WorkspaceState) {
        scope.launch {
            mutex.withLock {
                try {
                    var before = mutableState.value
                    val after =
                        repository.update { persisted ->
                            before = persisted
                            transform(persisted)
                        }
                    mutableState.value = after
                    recordWorkspaceChanges(statistics, before, after)
                    mutableError.value = null
                } catch (failure: IOException) {
                    mutableError.value = failure.message
                }
            }
        }
    }

    fun observe(snapshot: ThreadSnapshot) {
        latestSnapshot = snapshot
        reading.remember(snapshot)
        change { state ->
            val old = state.tabs.firstOrNull { it.address == snapshot.address }
            val tab = old?.copy(title = snapshot.subject) ?: OpenThread(snapshot.address, snapshot.subject)
            state
                .observe(snapshot)
                .visit(snapshot)
                .copy(tabs = state.tabs.filterNot { it.address == snapshot.address } + tab)
        }
    }

    fun storeOwn(post: BoardPost) {
        change {
            it.storePost(post, latestSnapshot?.takeIf { it.address == post.address.thread }?.subject, own = true)
        }
    }

    fun position(
        address: ThreadAddress,
        postId: Long,
        offset: Int,
    ) {
        if (!readingPreferences.rememberPosition) return
        reading.position(address, postId, offset)
        change { it.withReadingPosition(address, postId, offset) }
    }

    fun closeTab(address: ThreadAddress) =
        change { state ->
            state.copy(
                tabs = state.tabs.filterNot { it.address == address },
                lastRoute =
                    state.lastRoute.takeUnless {
                        it ==
                            AppRoute.thread(address)
                    },
            )
        }
}
