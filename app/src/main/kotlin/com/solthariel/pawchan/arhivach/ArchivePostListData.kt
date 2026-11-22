package com.solthariel.pawchan.arhivach

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.domain.archive.ArchiveThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun rememberArchivePostListData(
    thread: ArchiveThread?,
    words: String,
    prepare: suspend (ArchiveThread) -> ArchiveThreadPresentationUi = ::prepareArchiveThread,
): ArchivePostListData {
    var presentation by remember(thread?.id) { mutableStateOf<ArchiveThreadPresentationUi?>(null) }
    var data by remember(thread?.id) { mutableStateOf(ArchivePostListData(emptyList(), null)) }
    LaunchedEffect(thread, prepare) {
        presentation = thread?.let { prepare(it) }
    }
    LaunchedEffect(presentation, words) {
        val currentPresentation = presentation
        val posts = currentPresentation?.posts.orEmpty()
        val query = words.trim()
        val visiblePosts =
            withContext(Dispatchers.Default) {
                if (query.isBlank()) posts else posts.filter { it.matchesArchiveQuery(query) }
            }
        data = ArchivePostListData(visiblePosts, currentPresentation)
    }
    return data
}

private fun ArchivePostCardUi.matchesArchiveQuery(query: String): Boolean =
    document.plainText.contains(query, ignoreCase = true) ||
        post.subject?.contains(query, ignoreCase = true) == true ||
        post.author?.contains(query, ignoreCase = true) == true ||
        post.number?.contains(query, ignoreCase = true) == true

internal data class ArchivePostListData(
    val posts: List<ArchivePostCardUi>,
    val presentation: ArchiveThreadPresentationUi?,
)

private suspend fun prepareArchiveThread(thread: ArchiveThread): ArchiveThreadPresentationUi =
    withContext(Dispatchers.Default) { thread.toArchiveThreadPresentationUi() }
