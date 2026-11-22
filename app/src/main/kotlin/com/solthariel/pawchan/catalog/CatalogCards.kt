package com.solthariel.pawchan.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.FAST_SCROLL_CONTENT_INSET
import com.solthariel.pawchan.core.ui.FastScrollMode
import com.solthariel.pawchan.core.ui.LocalBottomOverlayInset
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import com.solthariel.pawchan.core.ui.PawchanFastScrollbar
import com.solthariel.pawchan.core.ui.PostSeparator
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import com.solthariel.pawchan.media.thumbnail.rememberThumbnailLoading
import com.solthariel.pawchan.navigation.saveableKey
import com.solthariel.pawchan.threads.ThreadListCard
import com.solthariel.pawchan.threads.ThreadListCardActions
import com.solthariel.pawchan.threads.ThreadListCardContentUi
import com.solthariel.pawchan.threads.ThreadListCardUi
import com.solthariel.pawchan.threads.ThreadListCountsUi
import com.solthariel.pawchan.threads.ThreadListStatusUi

@Composable
internal fun CatalogContent(
    state: CatalogUiState.Content,
    thumbnailLoader: ThumbnailLoader,
    onThreadClick: (ThreadAddress) -> Unit,
    onMediaClick: (List<MediaAttachmentUi>) -> Unit,
    onThreadLongClick: (CatalogThreadUi) -> Unit = {},
    onMediaLongClick: (MediaAttachmentUi) -> Unit = {},
) {
    val listState = rememberLazyListState()
    val thumbnailLoading = rememberThumbnailLoading(thumbnailLoader, listState)
    var appliedRefresh by androidx.compose.runtime.saveable.rememberSaveable {
        androidx.compose.runtime.mutableLongStateOf(state.refreshRevision)
    }
    androidx.compose.runtime.LaunchedEffect(state.refreshRevision) {
        if (appliedRefresh != state.refreshRevision) {
            appliedRefresh = state.refreshRevision
            listState.scrollToItem(0, 0)
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().clipToBounds(),
            contentPadding =
                PaddingValues(
                    top = com.solthariel.pawchan.core.ui.LocalTopOverlayInset.current,
                    bottom = LocalBottomOverlayInset.current + CATALOG_CARD_GAP.dp,
                ),
        ) {
            itemsIndexed(
                items = state.threads,
                key = { _, thread -> thread.address.saveableKey },
                contentType = { _, _ -> "catalog-thread" },
            ) { index, thread ->
                Column(Modifier.padding(horizontal = FAST_SCROLL_CONTENT_INSET)) {
                    CatalogThreadCard(
                        thread,
                        thumbnailLoading,
                        actions =
                            ThreadListCardActions(
                                onClick = { onThreadClick(thread.address) },
                                onOpenMedia = { attachments, _ -> onMediaClick(attachments) },
                                onMediaLongClick = onMediaLongClick,
                                onLongClick = { onThreadLongClick(thread) },
                            ),
                    )
                    PostSeparator(index, state.threads.lastIndex)
                }
            }
        }
        PawchanFastScrollbar(
            state = listState,
            inputGesture = thumbnailLoading.gesture,
            mode = FastScrollMode.ITEMS,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

/** The shared thread-list item used by catalog, favorites, and saved copies. */
@Composable
internal fun CatalogThreadCard(
    thread: CatalogThreadUi,
    thumbnailLoading: ThumbnailLoading,
    actions: ThreadListCardActions,
    modifier: Modifier = Modifier,
    sizeText: String = "",
) {
    val hideReplies = thread.content.text.authorPreview != null && LocalPostDisplayPreferences.current.hideMonkeyReplies
    ThreadListCard(
        card = thread.toThreadListCardUi(hideMonkeyReplies = hideReplies, sizeText = sizeText),
        thumbnailLoading = thumbnailLoading,
        actions = actions,
        modifier = modifier,
    )
}

internal fun CatalogThreadUi.toThreadListCardUi(
    hideMonkeyReplies: Boolean = false,
    sizeText: String = "",
): ThreadListCardUi =
    ThreadListCardUi(
        content =
            ThreadListCardContentUi(
                subject = displaySubject,
                comment = if (hideMonkeyReplies) content.text.authorPreview ?: comment else comment,
                note = note,
                media = attachments,
            ),
        counts =
            ThreadListCountsUi(
                repliesText = statistics.repliesText,
                repliesDescription = statistics.repliesDescription,
                filesText = statistics.filesText,
                filesDescription = statistics.filesDescription,
                sizeText = sizeText,
            ),
        status = ThreadListStatusUi(pinned, closed, endless),
        ageText = age.text,
        hiddenText =
            if (hidden) {
                flags.autoHide?.let { "Автоскрытие: ${it.label}" } ?: "Скрытый тред #$id"
            } else {
                null
            },
    )

private const val CATALOG_CARD_GAP = 6
