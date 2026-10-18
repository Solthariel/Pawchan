package com.solthariel.pawchan.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.ImageLoader
import coil3.compose.SubcomposeAsyncImage
import com.solthariel.pawchan.R
import com.solthariel.pawchan.media.AttachmentCaption
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.BindAttachmentImagePreload
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.calculateAttachmentGridMetrics

@Composable
internal fun CatalogAttachmentPicker(
    attachments: List<MediaAttachmentUi>,
    imageLoader: ImageLoader,
    onDismiss: () -> Unit,
    onSelected: (MediaAttachmentUi) -> Unit,
    onLongPress: (MediaAttachmentUi) -> Unit = {},
) {
    BindAttachmentImagePreload(attachments, imageLoader)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val dismissInteraction = remember { MutableInteractionSource() }
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = PICKER_SCRIM_ALPHA))
                    .clickable(
                        interactionSource = dismissInteraction,
                        indication = null,
                        onClick = onDismiss,
                    ).systemBarsPadding()
                    .padding(PICKER_SCREEN_PADDING),
            contentAlignment = Alignment.Center,
        ) {
            val metrics =
                calculateAttachmentGridMetrics(
                    itemCount = attachments.size,
                    availableWidth = maxWidth.value,
                    availableHeight = maxHeight.value,
                    maximumCellSize = PICKER_MAX_CELL_SIZE.value,
                    gap = PICKER_GAP.value,
                )
            val cellSize = metrics.cellSize.dp
            val rowWidth = cellSize * ATTACHMENT_GRID_COLUMNS.toFloat() + PICKER_GAP
            Column(verticalArrangement = Arrangement.spacedBy(PICKER_GAP)) {
                attachments.chunked(ATTACHMENT_GRID_COLUMNS).forEach { rowAttachments ->
                    Row(
                        modifier = Modifier.width(rowWidth),
                        horizontalArrangement = Arrangement.spacedBy(PICKER_GAP),
                    ) {
                        rowAttachments.forEach { attachment ->
                            CatalogAttachmentCell(
                                attachment = attachment,
                                imageLoader = imageLoader,
                                cellSize = cellSize,
                                onSelected = onSelected,
                                onLongPress = onLongPress,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogAttachmentCell(
    attachment: MediaAttachmentUi,
    imageLoader: ImageLoader,
    cellSize: Dp,
    onSelected: (MediaAttachmentUi) -> Unit,
    onLongPress: (MediaAttachmentUi) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(cellSize)
                .clip(RoundedCornerShape(PICKER_CORNER_RADIUS))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .combinedClickable(onClick = { onSelected(attachment) }, onLongClick = { onLongPress(attachment) }),
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = attachment.thumbnailUrl,
            imageLoader = imageLoader,
            contentDescription = attachment.fileName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (attachment.viewer == AttachmentViewer.VIDEO) {
            Text(
                text = stringResource(R.string.play_video),
                modifier =
                    Modifier
                        .background(Color.Black.copy(alpha = PLAY_BADGE_ALPHA), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                color = Color.White,
                fontSize = 24.sp,
            )
        }
        CatalogAttachmentMetadata(
            attachment = attachment,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
private fun CatalogAttachmentMetadata(
    attachment: MediaAttachmentUi,
    modifier: Modifier = Modifier,
) {
    AttachmentCaption(attachment, modifier)
}

private const val ATTACHMENT_GRID_COLUMNS = 2
private const val PICKER_SCRIM_ALPHA = 0.72f
private const val PLAY_BADGE_ALPHA = 0.65f
private val PICKER_MAX_CELL_SIZE = 120.dp
private val PICKER_GAP = 8.dp
private val PICKER_SCREEN_PADDING = 16.dp
private val PICKER_CORNER_RADIUS = 3.dp
