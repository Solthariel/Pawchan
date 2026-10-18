package com.solthariel.pawchan.media

import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.solthariel.pawchan.core.media.R
import com.solthariel.pawchan.core.ui.PawchanThinRowScrollbar
import com.solthariel.pawchan.media.MEDIA_ATTACHMENT_COLUMNS
import com.solthariel.pawchan.media.thumbnail.DeferredThumbnail
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import com.solthariel.pawchan.media.thumbnail.rememberThumbnailLoading

@Composable
fun MediaGallery(
    gallery: MediaGalleryContent,
    loader: ThumbnailLoader,
    actions: MediaGalleryActions,
    session: MediaGallerySession = rememberMediaGallerySession(),
    visible: Boolean = true,
) {
    if (!visible) return
    val hideStickers = com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences.current.hideStickers
    val visibleGallery = if (hideStickers) gallery.withoutStickers ?: MediaGalleryContent() else gallery
    val group = session.filter.group(visibleGallery)
    val close = {
        session.clearSelection()
        actions.onClose()
    }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.systemBarsPadding()) {
                GalleryToolbar(
                    GalleryToolbarState(
                        session.filter,
                        session.selecting,
                        session.selected.size,
                        group.keys.isNotEmpty() && session.selected.containsAll(group.keys),
                    ),
                    GalleryToolbarActions(
                        back = close,
                        chooseFilter = { session.filter = it },
                        toggleSelection = session::toggleSelection,
                        selectAll = { session.selectAll(group.keys) },
                    ),
                    visibleGallery,
                )
                GalleryGrid(
                    group,
                    loader,
                    session,
                    GalleryCellActions(
                        select = session::toggle,
                        open = actions.onOpen,
                        menu = actions.onMenu?.let { menu -> { menu(MediaMenuRequest(it, MediaMenuPlace.GALLERY)) } },
                    ),
                    Modifier.weight(1f),
                )
                if (session.selecting) {
                    val barActions = rememberSelectionBarActions(visibleGallery, actions, session)
                    GallerySelectionBar(session.selected.isNotEmpty(), barActions)
                }
            }
        }
    }
}

@Composable
private fun rememberSelectionBarActions(
    gallery: MediaGalleryContent,
    actions: MediaGalleryActions,
    session: MediaGallerySession,
): GallerySelectionBarActions {
    val context = LocalContext.current
    val chosen = { gallery.all.selectedItems(session.selected) }
    val favorited = stringResource(R.string.gallery_favorited)
    return GallerySelectionBarActions(
        download =
            actions.onDownloadAll?.let { download ->
                {
                    download(chosen())
                    session.clearSelection()
                }
            },
        copyLinks = {
            copyGalleryLinks(context, chosen())
            session.clearSelection()
        },
        favorite =
            actions.onFavoriteAll?.let { favorite ->
                {
                    favorite(chosen())
                    Toast.makeText(context, favorited, Toast.LENGTH_SHORT).show()
                    session.clearSelection()
                }
            },
    )
}

@Composable
internal fun GalleryGrid(
    group: MediaGalleryGroup,
    loader: ThumbnailLoader,
    session: MediaGallerySession,
    actions: GalleryCellActions,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val list = session.list
    val loading = rememberThumbnailLoading(loader, list)
    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 4.dp + bottomInset),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(
                group.rows,
                key = { rowIndex, _ -> group.keyAt(rowIndex * MEDIA_ATTACHMENT_COLUMNS) },
                contentType = { _, _ -> "gallery-row" },
            ) { rowIndex, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEachIndexed { column, attachment ->
                        val key = group.keyAt(rowIndex * MEDIA_ATTACHMENT_COLUMNS + column)
                        val onClick: (MediaAttachmentUi) -> Unit =
                            if (session.selecting) {
                                { actions.select(key) }
                            } else {
                                actions.open
                            }
                        // Долгое нажатие — меню файла, как в Dashchan; в режиме выделения тоже выделяет.
                        val onLongClick: ((MediaAttachmentUi) -> Unit)? =
                            if (session.selecting) onClick else actions.menu
                        GalleryCell(
                            attachment,
                            loading,
                            GalleryCellState(session.selecting, key in session.selected),
                            GalleryCellCallbacks(onClick, onLongClick, actions.open),
                            Modifier.weight(1f),
                        )
                    }
                    repeat(MEDIA_ATTACHMENT_COLUMNS - row.size) {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
        PawchanThinRowScrollbar(list, Modifier.align(Alignment.CenterEnd), loading.gesture)
    }
}

@Composable
private fun GalleryCell(
    attachment: MediaAttachmentUi,
    loading: ThumbnailLoading,
    state: GalleryCellState,
    callbacks: GalleryCellCallbacks,
    modifier: Modifier,
) {
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onLongClick = callbacks.longClick?.let { { it(attachment) } },
                onClick = { callbacks.click(attachment) },
            ),
    ) {
        DeferredThumbnail(
            attachment.thumbnailUrl,
            loading,
            attachment.fileName,
            Modifier.fillMaxSize(),
        )
        if (attachment.viewer == AttachmentViewer.VIDEO) {
            Text("▶", color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        AttachmentCaption(attachment, Modifier.align(Alignment.BottomStart))
        if (state.selecting) {
            GallerySelectionCircle(state.selected, Modifier.align(Alignment.TopStart).padding(6.dp))
            GalleryExpandButton({ callbacks.expand(attachment) }, Modifier.align(Alignment.TopEnd))
        }
    }
}

private data class GalleryCellCallbacks(
    val click: (MediaAttachmentUi) -> Unit,
    val longClick: ((MediaAttachmentUi) -> Unit)?,
    val expand: (MediaAttachmentUi) -> Unit,
)

internal data class GalleryCellActions(
    val select: (String) -> Unit,
    val open: (MediaAttachmentUi) -> Unit,
    val menu: ((MediaAttachmentUi) -> Unit)?,
)

private data class GalleryCellState(
    val selecting: Boolean,
    val selected: Boolean,
)

internal fun MediaGalleryGroup.selectedItems(selected: Set<String>): List<MediaAttachmentUi> =
    items.filterIndexed { index, _ -> keyAt(index) in selected }
