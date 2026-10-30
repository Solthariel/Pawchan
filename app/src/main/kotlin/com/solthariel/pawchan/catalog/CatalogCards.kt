package com.solthariel.pawchan.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.core.ui.FAST_SCROLL_CONTENT_INSET
import com.solthariel.pawchan.core.ui.FastScrollMode
import com.solthariel.pawchan.core.ui.PawchanFastScrollbar
import com.solthariel.pawchan.core.ui.PostSeparator
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiThreadCard
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.thumbnail.DeferredThumbnail
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import com.solthariel.pawchan.media.thumbnail.rememberThumbnailLoading
import com.solthariel.pawchan.navigation.chrome.APP_BOTTOM_BAR_HEIGHT
import com.solthariel.pawchan.navigation.saveableKey

@Composable
internal fun CatalogContent(
    state: CatalogUiState.Content,
    thumbnailLoader: ThumbnailLoader,
    onThreadClick: (ThreadAddress) -> Unit,
    onMediaClick: (List<MediaAttachmentUi>) -> Unit,
    onThreadLongClick: (CatalogThreadUi) -> Unit = {},
    onMediaLongClick: (MediaAttachmentUi) -> Unit = {},
) {
    val bubble = com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences.current.bubblePosts
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
                    bottom = APP_BOTTOM_BAR_HEIGHT + CATALOG_CARD_GAP,
                ),
            verticalArrangement = Arrangement.spacedBy(if (bubble) CATALOG_CARD_GAP else 0.dp),
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
                        onThreadClick,
                        CatalogMediaClicks(onMediaClick, onMediaLongClick),
                        Modifier,
                        onThreadLongClick,
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

/** Карточка треда каталога; та же карточка — в Избранном (треды и сохранённые копии). */
@Composable
internal fun CatalogThreadCard(
    thread: CatalogThreadUi,
    thumbnailLoading: ThumbnailLoading,
    onThreadClick: (ThreadAddress) -> Unit,
    media: CatalogMediaClicks,
    modifier: Modifier = Modifier,
    onThreadLongClick: (CatalogThreadUi) -> Unit = {},
) {
    val bubble = com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences.current.bubblePosts
    UiThreadCard(
        outlined = bubble,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = CATALOG_MIN_CARD_HEIGHT)
                .clip(if (bubble) MaterialTheme.shapes.medium else androidx.compose.ui.graphics.RectangleShape)
                .combinedClickable(
                    onClick = { onThreadClick(thread.address) },
                    onLongClick = { onThreadLongClick(thread) },
                ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(CATALOG_CARD_CONTENT_PADDING),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (thread.hidden) {
                Text(
                    thread.flags.autoHide?.let { "Автоскрытие: ${it.label}" } ?: "Скрытый тред #${thread.id}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            } else {
                CatalogPreview(thread, thumbnailLoading, media)
                CatalogThreadDetails(thread, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CatalogPreview(
    thread: CatalogThreadUi,
    thumbnailLoading: ThumbnailLoading,
    media: CatalogMediaClicks,
) {
    val attachments =
        com.solthariel.pawchan.media
            .visiblePostAttachments(thread.mediaDisplay)
    val preview = attachments.firstOrNull() ?: return
    Box(
        modifier =
            Modifier
                .size(CATALOG_PREVIEW_SIZE)
                .clip(MaterialTheme.shapes.small)
                .combinedClickable(
                    onClick = { media.open(attachments) },
                    // меню файла, который виден на миниатюре (первого).
                    onLongClick = { attachments.firstOrNull()?.let(media.longPress) },
                ),
        contentAlignment = Alignment.Center,
    ) {
        DeferredThumbnail(
            thumbnailUrl = preview.thumbnailUrl,
            loading = thumbnailLoading,
            contentDescription = preview.fileName,
            modifier = Modifier.fillMaxSize(),
        )
        if (attachments.size > 1) {
            CatalogMediaBadge(attachments.size)
        }
    }
}

@Composable
private fun CatalogMediaBadge(count: Int) {
    val description = stringResource(R.string.catalog_media_count, count)
    Box(
        modifier =
            Modifier
                .size(34.dp)
                .background(Color.Black.copy(alpha = MEDIA_BADGE_ALPHA), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        UiIcon(
            kind = UiIconKind.MEDIA_MULTIPLE,
            color = Color.White,
            modifier = Modifier.size(22.dp).semantics { contentDescription = description },
        )
    }
}

@Composable
private fun CatalogThreadDetails(
    thread: CatalogThreadUi,
    modifier: Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        thread.displaySubject?.let { subject ->
            Text(
                text = subject,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        CatalogComment(thread)
        thread.note?.let { note ->
            Text(
                note,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        CatalogMetadata(thread)
    }
}

@Composable
private fun CatalogComment(thread: CatalogThreadUi) {
    if (thread.comment.isBlank()) return
    val maxLines =
        if (thread.displaySubject == null) CATALOG_COMMENT_LINES else CATALOG_COMMENT_LINES_WITH_SUBJECT
    Text(
        text = thread.comment,
        modifier = Modifier.padding(top = 2.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun CatalogMetadata(thread: CatalogThreadUi) {
    CatalogCardMetadata(thread)
}

private const val CATALOG_COMMENT_LINES = 4
private const val CATALOG_COMMENT_LINES_WITH_SUBJECT = 3
private const val MEDIA_BADGE_ALPHA = 0.48f
private val CATALOG_MIN_CARD_HEIGHT = 80.dp
private val CATALOG_PREVIEW_SIZE = 88.dp
private val CATALOG_CARD_GAP = 8.dp
private val CATALOG_CARD_CONTENT_PADDING = 16.dp

internal data class CatalogMediaClicks(
    val open: (List<MediaAttachmentUi>) -> Unit,
    val longPress: (MediaAttachmentUi) -> Unit,
)
