package com.solthariel.pawchan.media

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.media.R
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_SUBTITLE_SIZE
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_TITLE_SIZE
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton

internal fun mediaToolbarSubtitle(
    attachment: MediaAttachmentUi,
    navigation: MediaViewerNavigation,
): String {
    val position = "${navigation.position}/${navigation.total}"
    return listOfNotNull(position, attachment.displayDimensions, attachment.displayByteSize).joinToString(", ")
}

@Composable
internal fun MediaToolbar(
    attachment: MediaAttachmentUi,
    navigation: MediaViewerNavigation,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onRefresh: () -> Unit,
    onMore: (() -> Unit)? = null,
) {
    Surface(color = Color.Transparent) {
        Row(
            Modifier.fillMaxWidth().height(APP_TOP_BAR_HEIGHT).padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UiToolbarButton(
                UiIconKind.BACK,
                "Назад",
                active = LocalMediaPresentation.current.backHighlighted,
                onClick = onClose,
            )
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(
                    attachment.fileName,
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontSize = APP_TOOLBAR_TITLE_SIZE,
                            lineHeight = APP_TOOLBAR_TITLE_SIZE,
                        ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    mediaToolbarSubtitle(attachment, navigation),
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontSize = APP_TOOLBAR_SUBTITLE_SIZE,
                            lineHeight = APP_TOOLBAR_SUBTITLE_SIZE,
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            UiToolbarButton(UiIconKind.SAVE, stringResource(R.string.media_save), onClick = onSave)
            UiToolbarButton(UiIconKind.REFRESH, stringResource(R.string.media_refresh), onClick = onRefresh)
            onMore?.let { UiToolbarButton(UiIconKind.MORE, stringResource(R.string.media_more), onClick = it) }
        }
    }
}

internal const val MEDIA_TOOLBAR_ALPHA = 0xAA / 255f
