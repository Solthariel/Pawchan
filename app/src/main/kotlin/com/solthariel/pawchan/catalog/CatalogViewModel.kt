package com.solthariel.pawchan.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.domain.catalog.CatalogRepository
import com.solthariel.pawchan.domain.filter.ContentFilterStatisticsRepository
import com.solthariel.pawchan.domain.filter.activeContentFilters
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.navigation.defaultBoardAddress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class CatalogViewModel(
    private val repository: CatalogRepository,
    val board: BoardAddress = defaultBoardAddress(),
    private val uiDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    hidden: Flow<Set<ThreadAddress>> = flowOf(emptySet()),
    rules: Flow<List<com.solthariel.pawchan.domain.filter.ContentFilterRule>> = flowOf(emptyList()),
    placeholders: Flow<Boolean> = flowOf(false),
    statistics: ContentFilterStatisticsRepository? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    private var sourceState: CatalogUiState = CatalogUiState.Loading
    private var searchQuery = ""
    private var showHidden = false
    private var revealFiltered = false
    private var hiddenThreads = emptySet<ThreadAddress>()
    private var filterEngine =
        com.solthariel.pawchan.domain.filter
            .ContentFilterEngine(emptyList(), com.solthariel.pawchan.domain.filter.ContentFilterPlace.CATALOG)
    private var loadJob: Job? = null
    private var speedJob: Job? = null
    private var latestSpeed: Int? = null
    private var refreshRevision = 0L
    private var presentationJob: Job? = null

    val state: StateFlow<CatalogUiState> = mutableState.asStateFlow()

    init {
        recordCatalogFilterStatistics(viewModelScope, state, statistics)
        viewModelScope.launch {
            placeholders.collect {
                showHidden = it
                presentSourceState()
            }
        }
        viewModelScope.launch {
            hidden.collect {
                hiddenThreads = it
                presentSourceState()
            }
        }
        viewModelScope.launch {
            rules.activeContentFilters().flowOn(uiDispatcher).collect {
                filterEngine =
                    withContext(uiDispatcher) {
                        com.solthariel.pawchan.domain.filter
                            .ContentFilterEngine(it, com.solthariel.pawchan.domain.filter.ContentFilterPlace.CATALOG)
                    }
                presentSourceState()
            }
        }
        load(resetScroll = false)
    }

    fun refresh() = load(resetScroll = true)

    private fun load(resetScroll: Boolean) {
        loadJob?.cancel()
        speedJob?.cancel()
        speedJob =
            viewModelScope.launch {
                repository.loadBoardSpeed(board)?.let { speed ->
                    currentCoroutineContext().ensureActive()
                    if (speed >= 0) {
                        latestSpeed = speed
                        sourceState = (sourceState as? CatalogUiState.Content)?.copy(boardSpeed = speed) ?: sourceState
                        presentSourceState()
                    }
                }
            }
        sourceState = sourceState.refreshing()
        presentSourceState()
        loadJob =
            viewModelScope.launch {
                val cachedBoardSpeed =
                    (sourceState as? CatalogUiState.Content)?.boardSpeed ?: 0
                val outcome = repository.loadCatalog(board)
                val previous = sourceState
                val speedAvailable =
                    (outcome as? com.solthariel.pawchan.domain.model.DomainOutcome.Success)
                        ?.value
                        ?.boardSpeedAvailable == true
                sourceState =
                    withContext(uiDispatcher) {
                        outcome
                            .toUiState()
                            .withViewerMedia()
                            .withBoardSpeedFallback(cachedBoardSpeed, speedAvailable)
                            .keepContentOnRefreshFailure(previous)
                    }
                if (sourceState is CatalogUiState.Content) {
                    if (outcome is com.solthariel.pawchan.domain.model.DomainOutcome.Success && resetScroll) {
                        refreshRevision++
                    }
                    sourceState =
                        (sourceState as CatalogUiState.Content).let { content ->
                            content.copy(
                                boardSpeed = latestSpeed ?: content.boardSpeed,
                                refresh = content.refresh.copy(revision = refreshRevision),
                            )
                        }
                }
                presentSourceState()
            }
    }

    fun refreshThreadAges() {
        presentSourceState()
    }

    fun toggleFilteredThreads() {
        revealFiltered = !revealFiltered
        presentSourceState()
    }

    fun setSearchQuery(query: String) {
        searchQuery = query
        presentSourceState(SEARCH_DEBOUNCE_MILLIS)
    }

    private fun presentSourceState(delayMillis: Long = 0L) {
        presentationJob?.cancel()
        val state = sourceState
        val query = searchQuery
        val hidden = hiddenThreads
        val engine = filterEngine
        val revealed = revealFiltered
        val placeholders = showHidden
        presentationJob =
            viewModelScope.launch {
                if (delayMillis > 0L) {
                    delay(delayMillis)
                }
                mutableState.value =
                    withContext(uiDispatcher) {
                        state.filtered(query, hidden, engine, revealed, placeholders)
                    }
            }
    }

    companion object {
        fun factory(
            repository: CatalogRepository,
            board: BoardAddress = defaultBoardAddress(),
            hidden: Flow<Set<ThreadAddress>> = flowOf(emptySet()),
            rules: Flow<List<com.solthariel.pawchan.domain.filter.ContentFilterRule>> = flowOf(emptyList()),
            placeholders: Flow<Boolean> = flowOf(false),
            statistics: ContentFilterStatisticsRepository? = null,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    CatalogViewModel(repository, board, Dispatchers.Default, hidden, rules, placeholders, statistics)
                }
            }
    }
}

private const val SEARCH_DEBOUNCE_MILLIS = 120L

private fun CatalogUiState.filtered(
    query: String,
    hidden: Set<ThreadAddress>,
    engine: com.solthariel.pawchan.domain.filter.ContentFilterEngine,
    reveal: Boolean,
    placeholders: Boolean,
): CatalogUiState =
    if (this is CatalogUiState.Content) {
        val now = currentEpochSeconds()
        val reasons =
            threads
                .mapNotNull { row ->
                    engine.thread(row.address.board, row.subject, row.content.filterText, row.id)?.let { rule ->
                        row.address to CatalogAutoHideReason(rule.id, rule.label)
                    }
                }.toMap()
        val matched = threads.filter { it.address in reasons }
        val searched =
            threads.matching(query).map { row ->
                val age = catalogThreadAge(row.age.createdAtEpochSeconds, now)
                val reason = reasons[row.address]?.takeUnless { reveal }
                row.copy(
                    hidden = row.address in hidden || reason != null,
                    age = age,
                    flags = row.flags.copy(autoHide = reason),
                )
            }
        copy(
            contentFilters =
                contentFilters.copy(
                    hiddenThreads = matched,
                    revealed = reveal,
                    allThreadsFiltered = threads.isNotEmpty() && threads.size == matched.size,
                    hits = catalogFilterHits(matched, reasons, hidden, reveal),
                ),
            threads = if (placeholders) searched else searched.filterNot { it.hidden },
        ).withViewerMedia()
    } else {
        this
    }

private fun CatalogUiState.refreshing(): CatalogUiState =
    when (this) {
        is CatalogUiState.Content -> copy(refresh = refresh.copy(loading = true, failure = null))
        is CatalogUiState.Error, CatalogUiState.Loading -> CatalogUiState.Loading
    }

internal fun CatalogUiState.withBoardSpeedFallback(
    cachedBoardSpeed: Int,
    speedAvailable: Boolean,
): CatalogUiState =
    if (this is CatalogUiState.Content && !speedAvailable && cachedBoardSpeed >= 0) {
        copy(boardSpeed = cachedBoardSpeed)
    } else {
        this
    }
