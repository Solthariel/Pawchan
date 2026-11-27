package com.solthariel.pawchan.thread

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import com.solthariel.pawchan.core.ui.OWN_POST_MARKER_COLOR
import com.solthariel.pawchan.core.ui.PostCardSurface
import com.solthariel.pawchan.core.ui.REPLY_TO_ME_MARKER_COLOR
import com.solthariel.pawchan.core.ui.postBodyTextStyle
import com.solthariel.pawchan.core.ui.postCollapseMaxLines
import com.solthariel.pawchan.core.ui.postContainerShape
import com.solthariel.pawchan.core.ui.postTextIsLimited
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
    val shape = postContainerShape()
    var expanded by rememberSaveable(post.id) { mutableStateOf(false) }
    val hideMonkey = post.richText.hasMonkeyReply && LocalPostDisplayPreferences.current.hideMonkeyReplies
    var limited by rememberSaveable(post.id, options.appearance.maxLines, hideMonkey) { mutableStateOf(false) }
    PostCardSurface(
        modifier =
            Modifier
                // Удалённый пост целиком полупрозрачный.
                .alpha(if (post.metadata.changes.deleted) DELETED_ALPHA else 1f)
                .semantics { selected = options.selected },
        color = postCardColor(options),
        contentModifier =
            Modifier
                .postCardMarker(
                    options.changes,
                    ownColor = OWN_POST_MARKER_COLOR,
                    replyColor = REPLY_TO_ME_MARKER_COLOR,
                ).postCardModifier(options, shape),
    ) {
        ThreadPostPrimaryContent(post, thumbnailLoading, attachmentActions, options)
        ThreadPostBody(post, options, expanded, onPostReference) { limited = it }
        ThreadPostBottomBar(
            options,
            limited && post.openingPost &&
                options.appearance.maxLines != com.solthariel.pawchan.core.ui.NO_POST_COLLAPSE,
        ) {
            expanded = true
            limited = false
        }
    }
}

@Composable
private fun postCardColor(options: ThreadPostOptions) =
    if (options.selected && !options.selectionOutsideRow) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
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
    PostTextContent(subject, tag, options.appearance.textSize) {
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
    val maxLines = postCollapseMaxLines(post.openingPost, expanded, options.appearance.maxLines)
    RichPostText(
        post.richText,
        onPostReference,
        presentation =
            RichPostPresentation(
                style = postBodyTextStyle(options.appearance.textSize),
                interactionsEnabled = !options.selecting,
                onLongPress = {
                    if (options.selecting) {
                        options.interactions.onSelect()
                    } else {
                        options.interactions.onMenu()
                    }
                },
                onQuoteSelection = options.interactions.onQuote,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { result ->
                    onLimitedChange(
                        maxLines != Int.MAX_VALUE && postTextIsLimited(result, expanded, maxLines),
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
    PostFooterActions(
        replyLabel = options.replyLabel,
        limited = limited,
        enabled = !options.selecting,
        onReplies = options.interactions.onReplies,
        onExpand = onExpand,
    )
}

private const val DELETED_ALPHA = 0.5f

internal data class ThreadAttachmentActions(
    val open: (MediaAttachmentUi) -> Unit,
    val download: (MediaAttachmentUi) -> Unit,
    val longPress: (MediaAttachmentUi) -> Unit,
)
