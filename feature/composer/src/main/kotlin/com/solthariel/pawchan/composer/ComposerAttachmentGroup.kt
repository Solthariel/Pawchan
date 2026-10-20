package com.solthariel.pawchan.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.solthariel.pawchan.feature.composer.R
import com.solthariel.pawchan.media.MediaCaption

@Composable
internal fun ComposerAttachmentStrip(
    files: List<ComposerAttachment>,
    imageLoader: ImageLoader,
    enabled: Boolean,
    onChange: (List<ComposerAttachment>) -> Unit,
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    if (files.isNotEmpty()) {
        ComposerMediaRows(files, imageLoader, enabled, onChange) { selectedId = it.id }
    }
    files.firstOrNull { it.id == selectedId }?.let { file ->
        key(file.id) {
            ComposerAttachmentViewer(
                file,
                imageLoader,
                enabled,
                onDismiss = { selectedId = null },
                onUpdate = { updated -> onChange(files.map { if (it.id == updated.id) updated else it }) },
                onRemove = {
                    selectedId = null
                    onChange(files.filterNot { it.id == file.id })
                },
            )
        }
    }
}

@Composable
private fun ComposerMediaRows(
    files: List<ComposerAttachment>,
    imageLoader: ImageLoader,
    enabled: Boolean,
    onChange: (List<ComposerAttachment>) -> Unit,
    onSelect: (ComposerAttachment) -> Unit,
) {
    val drag = remember { ComposerAttachmentDragState() }
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val metrics = calculateComposerStripMetrics(files.size, maxWidth.value)
        val grid =
            with(density) {
                ComposerDropGrid(maxWidth.toPx(), metrics.cellHeight.dp.toPx(), MEDIA_GAP.dp.toPx(), rtl)
            }
        Box(
            Modifier
                .fillMaxWidth()
                .height(
                    metrics.totalHeight.dp,
                ).composerAttachmentDragModifier(drag, grid, files, enabled, onChange),
        ) {
            ComposerMediaGrid(
                drag.arrange(files),
                imageLoader,
                enabled,
                metrics,
                drag.id,
                ComposerMediaActions(onChange, onSelect),
            )
            files.firstOrNull { it.id == drag.id }?.let { ComposerAttachmentDragOverlay(it, imageLoader, grid, drag) }
        }
    }
}

private data class ComposerMediaActions(
    val onChange: (List<ComposerAttachment>) -> Unit,
    val onSelect: (ComposerAttachment) -> Unit,
)

@Composable
private fun ComposerMediaGrid(
    files: List<ComposerAttachment>,
    imageLoader: ImageLoader,
    enabled: Boolean,
    metrics: ComposerStripMetrics,
    draggingId: String?,
    actions: ComposerMediaActions,
) {
    Column(Modifier.height(metrics.totalHeight.dp), verticalArrangement = Arrangement.spacedBy(MEDIA_GAP.dp)) {
        files.chunked(MEDIA_COLUMNS).forEachIndexed { rowIndex, row ->
            Row(
                Modifier.fillMaxWidth().height(metrics.cellHeight.dp),
                horizontalArrangement = Arrangement.spacedBy(MEDIA_GAP.dp),
            ) {
                repeat(MEDIA_COLUMNS) { index ->
                    Box(Modifier.weight(1f).fillMaxSize()) {
                        row.getOrNull(index)?.let { file ->
                            key(file.id) {
                                val order =
                                    Modifier.composerAttachmentOrderSemantics(
                                        rowIndex * MEDIA_COLUMNS + index,
                                        files,
                                        enabled,
                                        actions.onChange,
                                    )
                                val alpha = if (file.id == draggingId) 0f else 1f
                                ComposerMediaCell(file, imageLoader, enabled, order.alpha(alpha)) {
                                    actions.onSelect(file)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposerMediaCell(
    file: ComposerAttachment,
    imageLoader: ImageLoader,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = stringResource(R.string.attachment_open_file),
                onClick = onClick,
            ),
    ) {
        ComposerAttachmentPreview(file, imageLoader, Modifier.fillMaxSize())
        MediaCaption(file.typeAndSize, file.dimensions, Modifier.align(Alignment.BottomStart))
    }
}

@Composable
internal fun ComposerAttachmentPreview(
    file: ComposerAttachment,
    imageLoader: ImageLoader,
    modifier: Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        if (file.preview != null) {
            AsyncImage(
                model = file.preview,
                imageLoader = imageLoader,
                contentDescription = file.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
        if (file.video) {
            Text(
                stringResource(R.string.play_video),
                color = Color.White,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp)).padding(3.dp),
            )
        }
    }
}

internal data class ComposerStripMetrics(
    val rows: Int,
    val totalHeight: Float,
    val cellHeight: Float,
)

internal fun calculateComposerStripMetrics(
    itemCount: Int,
    availableWidth: Float,
): ComposerStripMetrics {
    require(itemCount in 1..MAX_MEDIA_FILES && availableWidth >= 0f)
    val rows = if (itemCount > MEDIA_COLUMNS) 2 else 1
    val totalHeight = ((availableWidth - MEDIA_GAP * (MEDIA_COLUMNS - 1)) / MEDIA_COLUMNS).coerceAtLeast(0f)
    val cellHeight = ((totalHeight - MEDIA_GAP * (rows - 1)) / rows).coerceAtLeast(0f)
    return ComposerStripMetrics(rows, totalHeight, cellHeight)
}

private const val MEDIA_COLUMNS = 4
private const val MAX_MEDIA_FILES = 8
private const val MEDIA_GAP = 6f
