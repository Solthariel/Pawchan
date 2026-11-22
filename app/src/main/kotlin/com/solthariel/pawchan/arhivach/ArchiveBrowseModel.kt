package com.solthariel.pawchan.arhivach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.data.network.ArhivachBrowser
import com.solthariel.pawchan.data.network.proxy.ScopedProxySelector
import com.solthariel.pawchan.domain.archive.ArchiveBrowser
import com.solthariel.pawchan.domain.archive.ArchiveMedia
import com.solthariel.pawchan.domain.archive.ArchiveTag
import com.solthariel.pawchan.domain.archive.ArchiveThread
import com.solthariel.pawchan.domain.archive.ArchiveThreadPage
import com.solthariel.pawchan.domain.archive.ArchiveThreadSummary
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.media.MediaAttachmentUi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class ArchiveThreadCardUi(
    val summary: ArchiveThreadSummary,
    val tags: String,
    val media: List<MediaAttachmentUi>,
)

private fun ArchiveThreadSummary.toCardUi(): ArchiveThreadCardUi =
    ArchiveThreadCardUi(
        summary = this,
        tags = tags.joinToString(" ") { it.title },
        media = media.map { it.toMediaAttachmentUi().withArchiveOrigin(id) },
    )

internal data class ArchiveSearchState(
    val query: String = "",
    val suggestions: List<ArchiveTag> = emptyList(),
    val error: Boolean = false,
)

internal data class ArchiveLoadState(
    val loading: Boolean = false,
    val error: String? = null,
)

internal data class ArchiveOpenThreadState(
    val id: Long? = null,
    val content: ArchiveThread? = null,
    val galleryEnabled: Boolean = false,
    val status: String? = null,
)

internal data class ArchivePaginationState(
    val startPage: Int = 0,
    val page: Int = 0,
    val feedKey: Int = 0,
    val loadingNextPage: Boolean = false,
    val nextPageError: String? = null,
)

internal data class ArchiveBrowseState(
    val search: ArchiveSearchState = ArchiveSearchState(),
    val tags: List<ArchiveTag> = emptyList(),
    val list: ArchiveThreadPage? = null,
    val threadCards: List<ArchiveThreadCardUi> = emptyList(),
    val openThread: ArchiveOpenThreadState = ArchiveOpenThreadState(),
    val load: ArchiveLoadState = ArchiveLoadState(),
    val pagination: ArchivePaginationState = ArchivePaginationState(),
) {
    val startPage: Int get() = pagination.startPage
    val page: Int get() = pagination.page
    val feedKey: Int get() = pagination.feedKey
    val loadingNextPage: Boolean get() = pagination.loadingNextPage
    val nextPageError: String? get() = pagination.nextPageError
    val selectedThread: Long? get() = openThread.id
    val thread: ArchiveThread? get() = openThread.content
}

internal class ArchiveBrowseModel(
    private val browser: ArchiveBrowser,
    private val release: () -> Unit = {},
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ArchiveBrowseState())
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null
    private var searchJob: Job? = null

    init {
        loadPage(0)
    }

    fun queryChanged(query: String) {
        searchJob?.cancel()
        mutableState.update { it.copy(search = ArchiveSearchState(query = query)) }
        if (query.isBlank()) return
        searchJob =
            viewModelScope.launch {
                delay(TAG_SEARCH_DELAY_MS)
                when (val result = browser.searchTags(query)) {
                    is DomainOutcome.Success -> {
                        mutableState.update { it.copy(search = it.search.copy(suggestions = result.value)) }
                    }

                    is DomainOutcome.Failure -> {
                        mutableState.update { it.copy(search = it.search.copy(error = true)) }
                    }
                }
            }
    }

    fun selectTag(tag: ArchiveTag) {
        val tags = state.value.tags
        if (tags.any { it.id == tag.id } || tags.size >= MAX_SELECTED_TAGS) return
        searchJob?.cancel()
        mutableState.update {
            it.copy(
                tags = tags + tag,
                search = ArchiveSearchState(),
                list = null,
                threadCards = emptyList(),
                pagination = it.pagination.copy(loadingNextPage = false, nextPageError = null),
            )
        }
        loadPage(0)
    }

    fun removeTag(tag: ArchiveTag) {
        mutableState.update {
            it.copy(
                tags = it.tags.filterNot { selected -> selected.id == tag.id },
                list = null,
                threadCards = emptyList(),
                pagination = it.pagination.copy(loadingNextPage = false, nextPageError = null),
            )
        }
        loadPage(0)
    }

    fun loadPage(page: Int) {
        requestPage(page, retainCurrent = false)
    }

    private fun requestPage(
        page: Int,
        retainCurrent: Boolean,
    ) {
        if (page < 0) return
        loadJob?.cancel()
        val previous = state.value
        val retain =
            retainCurrent && previous.list != null && previous.selectedThread == null && previous.startPage == page
        mutableState.update { current ->
            val pagination =
                if (retain) {
                    current.pagination.copy(loadingNextPage = false)
                } else {
                    current.pagination.copy(
                        startPage = page,
                        page = page,
                        feedKey = current.feedKey + 1,
                        loadingNextPage = false,
                        nextPageError = null,
                    )
                }
            current.copy(
                pagination = pagination,
                list = current.list.takeIf { retain },
                threadCards = current.threadCards.takeIf { retain }.orEmpty(),
                openThread = ArchiveOpenThreadState(),
                load = ArchiveLoadState(loading = true),
            )
        }
        val tags = state.value.tags.map { it.id }
        loadJob =
            viewModelScope.launch {
                val result = browser.loadThreads(tags, page)
                currentCoroutineContext().ensureActive()
                when (result) {
                    is DomainOutcome.Success -> {
                        val cards = withContext(backgroundDispatcher) { result.value.threads.map { it.toCardUi() } }
                        mutableState.update {
                            val pagination =
                                if (retain) {
                                    it.pagination.copy(page = page, feedKey = it.feedKey + 1, nextPageError = null)
                                } else {
                                    it.pagination
                                }
                            it.copy(
                                pagination = pagination,
                                list = result.value,
                                threadCards = cards,
                                load = ArchiveLoadState(),
                            )
                        }
                    }

                    is DomainOutcome.Failure -> {
                        mutableState.update {
                            it.copy(load = ArchiveLoadState(error = LOAD_FAILED))
                        }
                    }
                }
            }
    }

    fun loadNextPage() {
        val current = state.value
        if (!current.canLoadNextPage()) return
        val request = ArchiveNextPageRequest(current.startPage, current.page + 1, current.tags.map { it.id })
        loadJob?.cancel()
        mutableState.update {
            it.copy(pagination = it.pagination.copy(loadingNextPage = true, nextPageError = null))
        }
        loadJob =
            viewModelScope.launch {
                val result = browser.loadThreads(request.tags, request.page)
                currentCoroutineContext().ensureActive()
                when (result) {
                    is DomainOutcome.Success -> {
                        val cards = withContext(backgroundDispatcher) { result.value.threads.map { it.toCardUi() } }
                        mutableState.update { latest ->
                            if (!request.matches(latest)) {
                                latest
                            } else {
                                latest.copy(
                                    pagination =
                                        latest.pagination.copy(
                                            page = request.page,
                                            loadingNextPage = false,
                                            nextPageError = null,
                                        ),
                                    list = result.value,
                                    threadCards = (latest.threadCards + cards).distinctBy { it.summary.id },
                                )
                            }
                        }
                    }

                    is DomainOutcome.Failure -> {
                        mutableState.update { latest ->
                            if (!request.matches(latest)) {
                                latest
                            } else {
                                latest.copy(
                                    pagination =
                                        latest.pagination.copy(
                                            loadingNextPage = false,
                                            nextPageError = LOAD_FAILED,
                                        ),
                                )
                            }
                        }
                    }
                }
            }
    }

    fun openThread(id: Long) {
        loadJob?.cancel()
        mutableState.update { current ->
            val status =
                current.threadCards
                    .firstOrNull { it.summary.id == id }
                    ?.summary
                    ?.metadata
                    ?.status
                    ?: current.openThread.takeIf { it.id == id }?.status
            val retained = current.openThread.takeIf { it.id == id }
            current.copy(
                openThread = retained?.copy(status = status) ?: ArchiveOpenThreadState(id = id, status = status),
                load = ArchiveLoadState(loading = true),
                pagination = current.pagination.copy(loadingNextPage = false),
            )
        }
        loadJob =
            viewModelScope.launch {
                val result = browser.loadThread(id)
                currentCoroutineContext().ensureActive()
                when (result) {
                    is DomainOutcome.Success -> {
                        val galleryEnabled =
                            withContext(backgroundDispatcher) {
                                result.value.posts.any { it.media.isNotEmpty() }
                            }
                        mutableState.update {
                            it.copy(
                                openThread =
                                    it.openThread.copy(
                                        content = result.value,
                                        galleryEnabled = galleryEnabled,
                                    ),
                                load = ArchiveLoadState(),
                            )
                        }
                    }

                    is DomainOutcome.Failure -> {
                        mutableState.update {
                            it.copy(load = ArchiveLoadState(error = LOAD_FAILED))
                        }
                    }
                }
            }
    }

    fun refresh() {
        val current = state.value
        val selectedThread = current.selectedThread
        if (selectedThread != null) openThread(selectedThread) else requestPage(current.startPage, retainCurrent = true)
    }

    fun backToList() {
        loadJob?.cancel()
        mutableState.update { it.copy(openThread = ArchiveOpenThreadState(), load = ArchiveLoadState()) }
        if (state.value.list == null) loadPage(state.value.startPage)
    }

    override fun onCleared() {
        release()
    }

    companion object {
        fun factory(router: ScopedProxySelector? = null): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val browser = ArhivachBrowser.create(router = router)
                    ArchiveBrowseModel(browser, browser::close)
                }
            }
    }
}

internal const val MAX_SELECTED_TAGS = 5
private const val TAG_SEARCH_DELAY_MS = 350L
private const val LOAD_FAILED = "Ошибка при загрузке данных"
