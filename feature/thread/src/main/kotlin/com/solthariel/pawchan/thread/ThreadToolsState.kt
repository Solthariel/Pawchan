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

internal const val DEFAULT_THREAD_MAX_LINES = 20

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
    var selected by mutableStateOf<Set<Long>>(emptySet())
    var notice by mutableStateOf<String?>(null)
    var exporting by mutableStateOf(false)
    var postMenuId by mutableStateOf<Long?>(null)
    var reportTargets by mutableStateOf<Set<Long>?>(null)
    var replyPreviewPostId by mutableStateOf<Long?>(null)
    var previews by mutableStateOf<List<PostPreviewTarget>>(emptyList())
    private var job: Job? = null
    private var generation = 0

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

    fun setFlag(
        value: ThreadSearchFlag,
        content: ThreadUiState.Content?,
    ) {
        flag = value
        searchOpen = true
        menu = false
        advanced = false
        contents = false
        search(content)
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
        selected = if (id in selected) selected - id else selected + id
    }

    fun clearSelection() {
        selecting = false
        selected = emptySet()
    }

    fun cancel() {
        job?.cancel()
    }
}

private const val THREAD_SEARCH_DEBOUNCE_MILLIS = 120L
