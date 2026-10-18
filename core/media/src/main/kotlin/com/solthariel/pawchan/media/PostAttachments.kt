package com.solthariel.pawchan.media

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.media.MEDIA_ATTACHMENT_COLUMNS
import com.solthariel.pawchan.media.thumbnail.DeferredThumbnail
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading

@Composable
fun PostAttachments(
    display: PostMediaDisplay,
    thumbnailLoading: ThumbnailLoading,
    onAttachmentClick: (MediaAttachmentUi) -> Unit,
    onAttachmentDownload: (MediaAttachmentUi) -> Unit,
    onLongPress: ((MediaAttachmentUi) -> Unit)? = null,
) {
    visiblePostAttachments(display)
    val visible = display.visibleRows(com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences.current.hideStickers)
    if (visible.isEmpty()) return
    val actions = PostAttachmentActions(onAttachmentClick, onAttachmentDownload, onLongPress)
    Column(verticalArrangement = Arrangement.spacedBy(MEDIA_GAP)) {
        visible.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MEDIA_GAP)) {
                row.forEach { attachment ->
                    PostAttachment(
                        attachment,
                        thumbnailLoading,
                        actions,
                        Modifier.weight(1f),
                    )
                }
                repeat(MEDIA_ATTACHMENT_COLUMNS - row.size) {
                    Box(Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
fun PostAttachment(
    attachment: MediaAttachmentUi,
    thumbnailLoading: ThumbnailLoading,
    actions: PostAttachmentActions,
    modifier: Modifier,
) {
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = {
                    if (attachment.viewer == AttachmentViewer.NONE) {
                        actions.download(attachment)
                    } else {
                        actions.open(attachment)
                    }
                },
                onLongClick = { actions.longPress?.invoke(attachment) },
            ),
    ) {
        DeferredThumbnail(
            attachment.thumbnailUrl,
            thumbnailLoading,
            attachment.fileName,
            Modifier.fillMaxSize(),
        )
        if (attachment.viewer == AttachmentViewer.VIDEO) {
            Text("▶", color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        AttachmentCaption(attachment, Modifier.align(Alignment.BottomStart))
    }
}

@Composable
fun AttachmentCaption(
    attachment: MediaAttachmentUi,
    modifier: Modifier = Modifier,
) {
    MediaCaption(attachment.display.typeAndSize, attachment.displayDimensions, modifier)
}

data class PostAttachmentActions(
    val open: (MediaAttachmentUi) -> Unit,
    val download: (MediaAttachmentUi) -> Unit,
    val longPress: ((MediaAttachmentUi) -> Unit)?,
)

private val MEDIA_GAP = 6.dp
