package com.solthariel.pawchan.threads

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.core.ui.PostStatsText
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiSpacing
import com.solthariel.pawchan.core.ui.postContainerShape
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.PostMediaDisplay
import com.solthariel.pawchan.media.VideoThumbnailBadge
import com.solthariel.pawchan.media.rememberMediaThumbnailOrigin
import com.solthariel.pawchan.media.thumbnail.DeferredThumbnail
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import com.solthariel.pawchan.media.visiblePostAttachments

/** Source-neutral presentation; source-specific conversion stays with its screen. */
internal data class ThreadListCardUi(
    val content: ThreadListCardContentUi,
    val counts: ThreadListCountsUi,
    val status: ThreadListStatusUi,
    val ageText: String,
    val hiddenText: String? = null,
)

internal data class ThreadListCardContentUi(
    val subject: String?,
    val comment: String,
    val note: String?,
    val media: List<MediaAttachmentUi>,
)

internal data class ThreadListCountsUi(
    val repliesText: String,
    val repliesDescription: String,
    val filesText: String,
    val filesDescription: String,
    val sizeText: String,
)

internal data class ThreadListStatusUi(
    val pinned: Boolean,
    val closed: Boolean,
    val endless: Boolean,
    val additional: List<ThreadListStatusMarkerUi> = emptyList(),
)

internal data class ThreadListStatusMarkerUi(
    val icon: UiIconKind,
    val description: String,
    val tone: ThreadListStatusTone,
)

internal enum class ThreadListStatusTone {
    PRIMARY,
    SECONDARY,
    TERTIARY,
    ERROR,
    MUTED,
}

internal data class ThreadListCardActions(
    val onClick: () -> Unit,
    val onOpenMedia: (List<MediaAttachmentUi>, MediaAttachmentUi) -> Unit,
    val onMediaLongClick: ((MediaAttachmentUi) -> Unit)? = null,
    val onLongClick: (() -> Unit)? = null,
)

@Composable
internal fun ThreadListCard(
    card: ThreadListCardUi,
    thumbnailLoading: ThumbnailLoading,
    actions: ThreadListCardActions,
    modifier: Modifier = Modifier,
) {
    ThreadListCardFrame(modifier, card.hiddenText != null, actions) {
        val hiddenText = card.hiddenText
        if (hiddenText != null) {
            Text(
                hiddenText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            ThreadListPreview(card, thumbnailLoading, actions)
            ThreadListDetails(card, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ThreadListCardFrame(
    modifier: Modifier,
    hidden: Boolean,
    actions: ThreadListCardActions,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = postContainerShape()
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(if (hidden) HIDDEN_CARD_HEIGHT else CARD_HEIGHT)
                .clip(shape)
                .combinedClickable(onClick = actions.onClick, onLongClick = actions.onLongClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(CARD_CONTENT_PADDING),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun ThreadListPreview(
    card: ThreadListCardUi,
    thumbnailLoading: ThumbnailLoading,
    actions: ThreadListCardActions,
) {
    val attachments = visiblePostAttachments(PostMediaDisplay(card.content.media))
    val preview = attachments.firstOrNull() ?: return
    val origin = rememberMediaThumbnailOrigin(preview, thumbnailLoading, 10.dp)
    Box(
        modifier =
            Modifier
                .fillMaxHeight()
                .width(CARD_HEIGHT - CARD_CONTENT_PADDING * 2)
                .onGloballyPositioned(origin::place)
                .clip(RoundedCornerShape(10.dp))
                .combinedClickable(
                    onClick = {
                        origin.capture(enabled = attachments.size == 1)
                        actions.onOpenMedia(attachments, preview)
                    },
                    onLongClick =
                        actions.onMediaLongClick?.let { onLongClick ->
                            { attachments.firstOrNull()?.let(onLongClick) }
                        },
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
            ThreadListMediaBadge(attachments.size)
        } else if (preview.viewer == AttachmentViewer.VIDEO) {
            VideoThumbnailBadge()
        }
    }
}

@Composable
private fun ThreadListMediaBadge(count: Int) {
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
private fun ThreadListDetails(
    card: ThreadListCardUi,
    modifier: Modifier,
) {
    Column(modifier = modifier.fillMaxHeight()) {
        card.content.subject?.let { subject ->
            Text(
                text = subject,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ThreadListComment(card)
        }
        card.content.note?.let { note ->
            Text(
                note,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        ThreadListCardMetadata(card)
    }
}

@Composable
private fun ThreadListComment(card: ThreadListCardUi) {
    if (card.content.comment.isBlank()) return
    val maxLines =
        if (card.content.subject == null) COMMENT_LINES_WITHOUT_SUBJECT else COMMENT_LINES_WITH_SUBJECT
    Text(
        text = card.content.comment,
        modifier = Modifier.padding(top = 2.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
internal fun ThreadListCardMetadata(card: ThreadListCardUi) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(UiSpacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThreadListCount(card.counts.repliesText, card.counts.repliesDescription, R.drawable.ic_catalog_replies)
            ThreadListCount(card.counts.filesText, card.counts.filesDescription, R.drawable.ic_catalog_files)
            if (card.counts.sizeText.isNotEmpty()) PostStatsText(card.counts.sizeText, Modifier.weight(1f))
        }
        ThreadListStatusIcons(card.status, Modifier.padding(start = UiSpacing.extraSmall))
        if (card.ageText.isNotEmpty()) {
            PostStatsText(card.ageText, Modifier.padding(start = 6.dp).widthIn(max = 100.dp))
        }
    }
}

@Composable
private fun ThreadListCount(
    value: String,
    description: String,
    drawable: Int,
) {
    Row(
        Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(UiSpacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(drawable),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        PostStatsText(value)
    }
}

private const val COMMENT_LINES_WITHOUT_SUBJECT = 5
private const val COMMENT_LINES_WITH_SUBJECT = 4
private const val MEDIA_BADGE_ALPHA = 0.48f
private val CARD_HEIGHT = 110.dp
private val CARD_CONTENT_PADDING = UiSpacing.small
private val HIDDEN_CARD_HEIGHT = 48.dp
