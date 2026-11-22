package com.solthariel.pawchan.arhivach

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.PostSeparator
import com.solthariel.pawchan.infrastructure.MediaLoaders
import com.solthariel.pawchan.media.thumbnail.rememberThumbnailLoading
import com.solthariel.pawchan.thread.SharedPostPreviewStack
import com.solthariel.pawchan.thread.SharedPostPreviewWindow
import com.solthariel.pawchan.thread.SharedPreviewEntry

internal data class ArchivePreviewTarget(
    val number: Long,
    val replies: Boolean,
)

internal class ArchivePreviewState {
    val entries = mutableStateListOf<SharedPreviewEntry<ArchivePreviewTarget>>()
    private var sequence = 0L

    fun open(
        number: Long,
        replies: Boolean,
    ) {
        sequence++
        entries.add(SharedPreviewEntry("archive-preview-$sequence", ArchivePreviewTarget(number, replies)))
    }

    fun close() {
        if (entries.isNotEmpty()) entries.removeAt(entries.lastIndex)
    }

    fun clear() = entries.clear()
}

@Composable
internal fun ArchivePostPreviews(
    state: ArchivePreviewState,
    presentation: ArchiveThreadPresentationUi,
    loaders: MediaLoaders,
    actions: ArchivePostActions,
    threadStatus: String?,
) {
    SharedPostPreviewStack(state.entries.toList(), state::clear) { target ->
        ArchivePreviewWindow(target, presentation, loaders, actions, threadStatus, state::close)
    }
}

@Composable
private fun ArchivePreviewWindow(
    target: ArchivePreviewTarget,
    presentation: ArchiveThreadPresentationUi,
    loaders: MediaLoaders,
    actions: ArchivePostActions,
    threadStatus: String?,
    onClose: () -> Unit,
) {
    val posts =
        if (target.replies) {
            presentation.replies[target.number].orEmpty()
        } else {
            listOfNotNull(presentation.byNumber[target.number])
        }
    val list = rememberLazyListState()
    val thumbnails = rememberThumbnailLoading(loaders.thumbnailLoader, list)
    SharedPostPreviewWindow(onClose) {
        if (posts.isEmpty()) {
            Text(if (target.replies) "Ответов пока нет" else "Пост отсутствует или удалён", Modifier.padding(12.dp))
        } else {
            LazyColumn(
                modifier = Modifier.testTag("preview-list"),
                state = list,
                contentPadding = PaddingValues(8.dp),
            ) {
                itemsIndexed(
                    posts,
                    key = { _, card -> "${card.ordinal}:${card.post.number.orEmpty()}" },
                ) { index, card ->
                    Column {
                        ArchivePostRow(card, thumbnails, actions, threadStatus, presentation)
                        PostSeparator(index, posts.lastIndex)
                    }
                }
            }
        }
    }
}
