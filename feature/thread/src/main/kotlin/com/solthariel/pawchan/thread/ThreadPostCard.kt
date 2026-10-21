package com.solthariel.pawchan.thread

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.core.ui.PostStatsText
import com.solthariel.pawchan.core.ui.postContainerShape
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.PostAttachments
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading

@Composable
internal fun ThreadPostCard(
    post: ThreadPostUi,
    thumbnailLoading: ThumbnailLoading,
    attachmentActions: ThreadAttachmentActions,
    onPostReference: (Long) -> Unit,
    options: ThreadPostOptions = ThreadPostOptions(),
) {
    val bubble = com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences.current.bubblePosts
    val shape = postContainerShape(bubble)
    var expanded by rememberSaveable(post.id) { mutableStateOf(false) }
    var limited by rememberSaveable(post.id, options.appearance.maxLines) { mutableStateOf(false) }
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                // Удалённый пост целиком полупрозрачный, как в Dashchan.
                .alpha(if (post.metadata.changes.deleted) DELETED_ALPHA else 1f)
                .semantics { selected = options.selected },
        shape = shape,
        color = postCardColor(options),
        border = if (bubble) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        val markerColor =
            when {
                options.changes.own -> MaterialTheme.colorScheme.primary
                options.changes.replyToMe -> MaterialTheme.colorScheme.tertiary
                else -> null
            }
        Column(
            modifier =
                Modifier
                    .postCardMarker(markerColor)
                    .postCardModifier(options, shape),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ThreadPostPrimaryContent(
                post,
                thumbnailLoading,
                attachmentActions,
                options,
            )
            ThreadPostBody(post, options, expanded, onPostReference) { limited = it }
            ThreadPostBottomBar(options, limited) {
                expanded = true
                limited = false
            }
        }
    }
}

@Composable
private fun postCardColor(options: ThreadPostOptions) =
    if (options.selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface

private fun Modifier.postCardMarker(color: androidx.compose.ui.graphics.Color?): Modifier =
    if (color == null) {
        this
    } else {
        drawBehind {
            drawLine(
                color = color,
                start = Offset(1.5.dp.toPx(), 10.dp.toPx()),
                end = Offset(1.5.dp.toPx(), size.height - 10.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
            )
        }
    }

private fun Modifier.postCardModifier(
    options: ThreadPostOptions,
    shape: Shape,
): Modifier =
    this
        .fillMaxWidth()
        .clip(shape)
        .combinedClickable(
            onClick = { if (options.selecting) options.interactions.onSelect() },
            onLongClick = {
                if (options.selecting) {
                    options.interactions.onSelect()
                } else {
                    options.interactions.onMenu()
                }
            },
        ).padding(8.dp)

@Composable
private fun ThreadPostPrimaryContent(
    post: ThreadPostUi,
    thumbnailLoading: ThumbnailLoading,
    attachmentActions: ThreadAttachmentActions,
    options: ThreadPostOptions,
) {
    ThreadPostHeader(post, thumbnailLoading)
    PostAttachments(
        post.mediaDisplay,
        thumbnailLoading,
        { attachment ->
            if (options.selecting) options.interactions.onSelect() else attachmentActions.open(attachment)
        },
        { attachment ->
            if (options.selecting) options.interactions.onSelect() else attachmentActions.download(attachment)
        },
        { attachment ->
            if (options.selecting) options.interactions.onSelect() else attachmentActions.longPress(attachment)
        },
    )
}

/** Тема — жирная первая строка текста того же размера, без отдельного отступа. */
@Composable
private fun ThreadPostBody(
    post: ThreadPostUi,
    options: ThreadPostOptions,
    expanded: Boolean,
    onPostReference: (Long) -> Unit,
    onLimitedChange: (Boolean) -> Unit,
) {
    val subject = post.metadata.display.subject
    val tag = post.metadata.display.tag
    if (subject == null && tag == null && post.document.plainText.isBlank()) return
    Column {
        // Тег треда — строкой над темой, как в Dashchan, но спокойнее: чуть крупнее текста, обычный вес.
        tag?.let {
            Text(
                "/$it/",
                Modifier.padding(bottom = TAG_GAP),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (options.appearance.textSize + TAG_SIZE_EXTRA).sp,
                lineHeight = (options.appearance.textSize + TAG_SIZE_EXTRA + POST_LINE_HEIGHT_EXTRA).sp,
            )
        }
        subject?.let {
            Text(
                it,
                fontWeight = FontWeight.Bold,
                fontSize = options.appearance.textSize.sp,
                lineHeight = (options.appearance.textSize + POST_LINE_HEIGHT_EXTRA).sp,
            )
        }
        ThreadPostComment(post, options, expanded, onPostReference, onLimitedChange)
    }
}

@Composable
private fun ThreadPostComment(
    post: ThreadPostUi,
    options: ThreadPostOptions,
    expanded: Boolean,
    onPostReference: (Long) -> Unit,
    onLimitedChange: (Boolean) -> Unit,
) {
    if (post.document.plainText.isBlank()) return
    RichPostText(
        post.richText,
        onPostReference,
        presentation =
            RichPostPresentation(
                style =
                    TextStyle(
                        fontSize = options.appearance.textSize.sp,
                        lineHeight = (options.appearance.textSize + POST_LINE_HEIGHT_EXTRA).sp,
                    ),
                interactionsEnabled = !options.selecting,
                onLongPress = {
                    if (options.selecting) {
                        options.interactions.onSelect()
                    } else {
                        options.interactions.onMenu()
                    }
                },
                onQuoteSelection = options.interactions.onQuote,
                maxLines = if (expanded) Int.MAX_VALUE else options.appearance.maxLines,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { result ->
                    onLimitedChange(
                        !expanded && (result.hasVisualOverflow || result.lineCount > options.appearance.maxLines),
                    )
                },
            ),
        spoilerKey = post.id,
    )
}

@Composable
private fun ThreadPostBottomBar(
    options: ThreadPostOptions,
    limited: Boolean,
    onExpand: () -> Unit,
) {
    if (options.replyLabel == null && !limited) return
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        // Как в Dashchan: кнопки стоят рядом слева, последняя занимает остаток строки.
        options.replyLabel?.let { label ->
            PostRepliesFooter(
                label,
                !options.selecting,
                options.interactions.onReplies,
                if (limited) Modifier else Modifier.weight(1f),
            )
        }
        if (limited) {
            PostStatsText(
                "Раскрыть",
                Modifier
                    .weight(1f)
                    .clickable(
                        enabled = !options.selecting,
                        role = Role.Button,
                        onClick = onExpand,
                    ),
            )
        }
    }
}

private const val POST_LINE_HEIGHT_EXTRA = 5
private const val TAG_SIZE_EXTRA = 3
private const val DELETED_ALPHA = 0.5f
private val TAG_GAP = 6.dp

internal data class ThreadAttachmentActions(
    val open: (MediaAttachmentUi) -> Unit,
    val download: (MediaAttachmentUi) -> Unit,
    val longPress: (MediaAttachmentUi) -> Unit,
)
