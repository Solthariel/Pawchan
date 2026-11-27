package com.solthariel.pawchan.thread

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class ThreadDialog {
    NONE,
    APPEARANCE,
    GALLERY,
    REPORT,
    ARCHIVE,
    ARHIVACH,
    HIDDEN_RULES,
}

internal data class ThreadAppearance(
    val textSize: Int = DEFAULT_THREAD_TEXT_SIZE,
    val maxLines: Int = DEFAULT_THREAD_MAX_LINES,
)

/** What of the in-thread search is kept while the thread is off screen (another tab, a linked thread). */
internal data class ThreadSearchMemory(
    val open: Boolean = false,
    val query: String = "",
    val flag: String? = null,
)

internal const val DEFAULT_THREAD_MAX_LINES = 20
internal const val DEFAULT_THREAD_TEXT_SIZE = 12

internal class ThreadToolsState(
    private val scope: CoroutineScope,
    private val searchDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    var readingReturn by mutableStateOf<ThreadReadingPosition?>(null)
    var onReadingReturn: () -> Unit = {}
    var menu by mutableStateOf(false)
    var advanced by mutableStateOf(false)

    /** Подменю «Содержимое ›». */
    var contents by mutableStateOf(false)
    var dialog by mutableStateOf(ThreadDialog.NONE)
    var searchOpen by mutableStateOf(false)
    var searching by mutableStateOf(false)
    var query by mutableStateOf("")
    var flag by mutableStateOf<ThreadSearchFlag?>(null)
    var results by mutableStateOf<List<Long>>(emptyList())
    var resultIndex by mutableIntStateOf(-1)
    var selecting by mutableStateOf(false)
    val shots = ThreadShotCapture(scope)
    var selected by mutableStateOf<Set<Long>>(emptySet())
    var notice by mutableStateOf<String?>(null)
    var exporting by mutableStateOf(false)
    var postMenuId by mutableStateOf<Long?>(null)
    var reportTargets by mutableStateOf<Set<Long>?>(null)
    var replyPreviewPostId by mutableStateOf<Long?>(null)
    var previews by mutableStateOf<List<PostPreviewTarget>>(emptyList())
    private var job: Job? = null
    private var generation = 0

    fun openPostPreview(
        target: PostPreviewTarget,
        local: ThreadUiState.Content?,
    ) {
        val isLocal = local != null && (target.address == null || target.address == local.address)
        if (isLocal && !target.replies && requireNotNull(local).postForPreview(target.postId) == null) {
            notice = "Сообщение не найдено"
        } else {
            previews = previews + target
        }
    }

    fun search(
        content: ThreadUiState.Content?,
        input: String = query,
    ) {
        query = input
        job?.cancel()
        val expected = ++generation
        val posts = content?.posts
        if (posts == null) {
            searching = false
            results = emptyList()
            return
        }
        val activeFlag = flag
        searching = true
        job =
            scope.launch {
                try {
                    delay(THREAD_SEARCH_DEBOUNCE_MILLIS)
                    val ids =
                        withContext(searchDispatcher) {
                            val terms = parseThreadSearch(input)
                            posts
                                .filter { post ->
                                    ensureActive()
                                    matchesThreadSearch(post, terms, activeFlag)
                                }.map(ThreadPostUi::id)
                        }
                    if (generation == expected) {
                        results = ids
                        resultIndex = -1
                    }
                } finally {
                    if (generation == expected) searching = false
                }
            }
    }

    /** Search survives leaving the screen: the restored query runs again once the thread content is shown. */
    fun restoreSearch(saved: ThreadSearchMemory) {
        searchOpen = saved.open
        query = saved.query
        flag = saved.flag?.let { name -> ThreadSearchFlag.entries.firstOrNull { it.name == name } }
    }

    fun searchMemory(): ThreadSearchMemory = ThreadSearchMemory(searchOpen, query, flag?.name)

    /** Choosing a flag opens the search; the screen effect starts the search for the current content. */
    fun selectFlag(value: ThreadSearchFlag) {
        flag = value
        searchOpen = true
        menu = false
        advanced = false
        contents = false
    }

    fun closeSearch() {
        job?.cancel()
        generation++
        searching = false
        searchOpen = false
        query = ""
        flag = null
        results = emptyList()
    }

    fun toggleSelection(id: Long) {
        selecting = true
        if (id in selected) {
            selected -= id
            shots.deselect(id)
        } else {
            selected += id
            shots.request(id) { notice = it }
        }
    }

    fun clearSelection() {
        selecting = false
        selected = emptySet()
        shots.clear()
    }

    fun cancel() {
        shots.clear()
        job?.cancel()
    }
}

private const val THREAD_SEARCH_DEBOUNCE_MILLIS = 120L
