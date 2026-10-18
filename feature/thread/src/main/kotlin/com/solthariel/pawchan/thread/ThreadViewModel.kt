package com.solthariel.pawchan.thread

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.domain.filter.ContentFilterStatisticsRepository
import com.solthariel.pawchan.domain.filter.activeContentFilters
import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.postpreferences.PostPreferencesRepository
import com.solthariel.pawchan.domain.settings.AppSettingsRepository
import com.solthariel.pawchan.domain.thread.CachedThreadRepository
import com.solthariel.pawchan.domain.thread.ReportingThreadRepository
import com.solthariel.pawchan.domain.thread.ThreadHistoryRepository
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.domain.thread.withRestoredPosts
import com.solthariel.pawchan.domain.thread.withoutDeleted
import com.solthariel.pawchan.domain.thread.withoutOldDeleted
import com.solthariel.pawchan.domain.watch.CheckWatchedThread
import com.solthariel.pawchan.domain.watch.ThreadWatchRepository
import com.solthariel.pawchan.thread.watch.ForegroundThreadWatch
import com.solthariel.pawchan.thread.watch.ThreadWatcherController
import com.solthariel.pawchan.thread.watch.ThreadWatcherPoll
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class ThreadViewModel(
    private val address: ThreadAddress,
    private val repository: ThreadRepository,
    persistence: ThreadPersistence,
    private val environment: ThreadViewModelEnvironment = ThreadViewModelEnvironment(),
) : ThreadSession() {
    override val sessionAddress: ThreadAddress get() = address

    private val ownPostIds = mutableSetOf<Long>()
    private val library = persistence.library
    private val watches = persistence.watches
    private val workspaceEvents = persistence.workspaceEvents
    private val visits = ThreadVisitRecorder(address, library, workspaceEvents, environment.nowMillis)
    private val storedHistory = persistence.history
    private val foregroundWatch =
        ForegroundThreadWatch(
            address = address,
            watches = watches,
            checkWatchedThread = CheckWatchedThread(repository),
        )
    private val mutableState = MutableStateFlow<ThreadUiState>(ThreadUiState.Loading)
    private val requestMutex = Mutex()
    private var loadJob: Job? = null
    private var reopenJob: Job? = null
    private var watchPersistenceJob: Job? = null
    private var opened = false
    private val reopenCache = ThreadReopenCache(address, repository, watches)

    override val postPreferences =
        ThreadPostPreferencesController(
            address,
            persistence.postPreferences,
            PostContentAccess({ mutableState.value }, { mutableState.value = it }),
            environment.uiDispatcher,
        )

    private val favorites =
        ThreadFavoriteController(
            viewModelScope,
            address,
            library,
            PostContentAccess({ mutableState.value }, { mutableState.value = it }),
            environment.nowMillis,
        )

    private val revealedFilters = MutableStateFlow<Set<Long>>(emptySet())

    override fun revealContentFilter(id: Long) {
        revealedFilters.value = revealedFilters.value + id
    }

    private val activeContentRules =
        (persistence.appSettings?.appSettings?.map { it.contentFilters } ?: flowOf(emptyList())).activeContentFilters()

    override val state: StateFlow<ThreadUiState> =
        combine(
            mutableState,
            activeContentRules,
            revealedFilters,
        ) { content, rules, revealed -> content.filteredContent(rules, revealed) }
            .flowOn(environment.uiDispatcher)
            .stateIn(viewModelScope, SharingStarted.Eagerly, ThreadUiState.Loading)
    override val reports =
        ThreadReportController(
            viewModelScope,
            address,
            repository as? ReportingThreadRepository,
        )
    override val watcher =
        ThreadWatcherController(
            scope = viewModelScope,
            intervalMillis = environment.watchIntervalMillis,
            poll = ::pollForUpdates,
        )

    init {
        com.solthariel.pawchan.thread.watch
            .bindAutomaticThreadRefresh(viewModelScope, persistence.appSettings, watcher)
        recordThreadFilterStatistics(viewModelScope, state, persistence.filterStatistics)
        refresh()
        watchPersistenceJob =
            viewModelScope.launch {
                val subscription = watches.load(address)
                if (subscription is DomainOutcome.Success && subscription.value != null) {
                    watcher.setEnabled(true)
                }
            }
    }

    /**
     * Первое открытие экрана: тред, уже прочитанный в этой сессии, показывается из памяти
     * без запроса в сеть, если наблюдатель не нашёл в нём новых постов (как в Dashchan).
     */
    override fun refresh() {
        val reopening = !opened
        opened = true
        loadJob?.cancel()
        mutableState.value = mutableState.value.refreshing()
        loadJob =
            viewModelScope.launch {
                val cached = showCachedThread()
                if (reopening && cached != null && reopenCache.isCurrent(cached)) {
                    visits.record(DomainOutcome.Success(cached))
                    return@launch
                }
                val outcome = requestMutex.withLock { repository.loadThread(address) }
                val favorite = favorites.load(outcome)
                val previous = mutableState.value
                val own = ownPostIds.toSet()
                mutableState.value =
                    favorites.decorate(
                        postPreferences.prepare(
                            withContext(environment.uiDispatcher) {
                                outcome
                                    .toThreadUiState(
                                        favorite,
                                    ).withPostHistory(previous, own)
                                    .keepContentOnRefreshFailure(previous)
                            },
                        ),
                    )
                visits.record(outcome)
            }
    }

    /**
     * Повторный показ уже загруженного треда: посты остаются на экране, как в Dashchan;
     * запрос в сеть идёт фоном и только если копия в памяти устарела по данным наблюдателя.
     */
    override fun reopen() {
        if (loadJob?.isActive == true || reopenJob?.isActive == true) return
        if (mutableState.value !is ThreadUiState.Content) return
        reopenJob =
            viewModelScope.launch {
                val cached = (repository as? CachedThreadRepository)?.cachedThread(address)
                if (cached == null || !reopenCache.isCurrent(cached)) refresh()
            }
    }

    private suspend fun showCachedThread(): ThreadSnapshot? {
        val cached =
            (repository as? CachedThreadRepository)
                ?.takeIf { mutableState.value == ThreadUiState.Loading }
                ?.cachedThread(address) ?: return null
        val favorite = favorites.load(DomainOutcome.Success(cached))
        mutableState.value =
            favorites.decorate(
                postPreferences.prepare(
                    withContext(environment.uiDispatcher) { DomainOutcome.Success(cached).toThreadUiState(favorite) },
                ),
            )
        return cached
    }

    override fun onPostSubmitted(post: BoardPost) {
        if (post.address.thread != address) return
        val id = post.address.post.value
        ownPostIds.add(id)
        workspaceEvents?.storeOwn(post)
        viewModelScope.launch {
            postPreferences.markPosted(id)
            refresh()
        }
    }

    override val history: ThreadHistoryActions? =
        storedHistory?.let { stored ->
            ThreadHistoryActions(
                restore = { archived, done -> editHistory(stored, done) { it.withRestoredPosts(archived) } },
                clearOld = { editHistory(stored) { it.withoutOldDeleted() } },
                clearDeleted = { editHistory(stored) { it.withoutDeleted() } },
                // «Стереть»: история забывается, тред загружается заново без удалённых постов.
                erase = {
                    viewModelScope.launch {
                        requestMutex.withLock { stored.erase(address) }
                        refresh()
                    }
                },
            )
        }

    /**
     * Правка истории треда: «Очистить старые/удалённые» и «Восстановить из Архивача»
     * (недостающие посты добавляются как удалённые). [done] получает изменение числа постов.
     */
    private fun editHistory(
        stored: ThreadHistoryRepository,
        done: (Int) -> Unit = {},
        transform: (ThreadSnapshot) -> ThreadSnapshot,
    ) {
        viewModelScope.launch {
            requestMutex.withLock {
                val content = mutableState.value as? ThreadUiState.Content ?: return@withLock
                val history = stored.load(address) ?: return@withLock
                val edited = transform(history)
                if (edited != history) {
                    stored.save(edited)
                    showSnapshot(edited, content.isFavorite)
                }
                done(edited.posts.size - history.posts.size)
            }
        }
    }

    override fun setFavorite(favorite: Boolean) = favorites.set(favorite)

    override fun refreshFavorite() = favorites.refresh()

    override fun toggleWatcher() {
        val content = mutableState.value as? ThreadUiState.Content ?: return
        val enabled = !watcher.state.value.enabled
        watcher.setEnabled(enabled)
        watchPersistenceJob?.cancel()
        watchPersistenceJob =
            viewModelScope.launch {
                val outcome =
                    if (enabled) {
                        watches.enable(
                            address = address,
                            subject = content.subject,
                            lastKnownPostId = content.latestPostId(),
                            enabledAtEpochMillis = environment.nowMillis(),
                        )
                    } else {
                        watches.disable(address)
                    }
                if (outcome is DomainOutcome.Failure && watcher.state.value.enabled == enabled) {
                    watcher.setEnabled(!enabled)
                }
            }
    }

    private suspend fun pollForUpdates(): ThreadWatcherPoll? {
        val previous =
            if (loadJob?.isActive == true) {
                null
            } else {
                mutableState.value as? ThreadUiState.Content
            }
        val update =
            previous?.let {
                requestMutex.withLock {
                    foregroundWatch.check() ?: com.solthariel.pawchan.thread.watch.loadAutomaticUpdate(
                        repository,
                        address,
                        it.latestPostId(),
                    )
                }
            }
        update?.snapshot?.let { snapshot ->
            showSnapshot(snapshot, requireNotNull(previous).isFavorite)
        }
        return update?.poll
    }

    private suspend fun showSnapshot(
        snapshot: ThreadSnapshot,
        favorite: Boolean,
    ) {
        workspaceEvents?.observe(snapshot)
        val previous = mutableState.value
        val own = ownPostIds.toSet()
        mutableState.value =
            favorites.decorate(
                postPreferences.prepare(
                    withContext(environment.uiDispatcher) {
                        DomainOutcome
                            .Success(snapshot)
                            .toThreadUiState(favorite)
                            .withPostHistory(previous, own)
                    },
                ),
            )
    }

    companion object {
        fun factory(
            address: ThreadAddress,
            repository: ThreadRepository,
            persistence: ThreadPersistence,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    ThreadViewModel(
                        address = address,
                        repository = repository,
                        persistence = persistence,
                        environment = ThreadViewModelEnvironment(uiDispatcher = Dispatchers.Default),
                    )
                }
            }
    }
}

data class ThreadPersistence(
    val library: ThreadLibraryRepository,
    val watches: ThreadWatchRepository,
    val postPreferences: PostPreferencesRepository? = null,
    val workspaceEvents: ThreadWorkspaceEvents? = null,
    val history: ThreadHistoryRepository? = null,
    val appSettings: AppSettingsRepository? = null,
    val filterStatistics: ContentFilterStatisticsRepository? = null,
)

internal data class ThreadViewModelEnvironment(
    val nowMillis: () -> Long = System::currentTimeMillis,
    val watchIntervalMillis: Long = ThreadWatcherController.DEFAULT_WATCH_INTERVAL_MILLIS,
    val uiDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
)

private fun ThreadUiState.Content.latestPostId(): PostId? = posts.maxOfOrNull(ThreadPostUi::id)?.let(PostId::parse)

private fun ThreadUiState.refreshing(): ThreadUiState =
    when (this) {
        is ThreadUiState.Content -> {
            copy(
                presentation = presentation.copy(isRefreshing = true, refreshFailure = null),
            )
        }

        is ThreadUiState.Error, ThreadUiState.Loading -> {
            ThreadUiState.Loading
        }
    }
