package com.solthariel.pawchan.media

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.solthariel.pawchan.core.media.R
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.core.ui.UiTopAppBar

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
    UiTopAppBar(
        title = {
            Column {
                Text(
                    attachment.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    mediaToolbarSubtitle(attachment, navigation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            UiToolbarButton(
                UiIconKind.BACK,
                "Назад",
                active = LocalMediaPresentation.current.backHighlighted,
                onClick = onClose,
            )
        },
        actions = {
            UiToolbarButton(UiIconKind.SAVE, stringResource(R.string.media_save), onClick = onSave)
            UiToolbarButton(UiIconKind.REFRESH, stringResource(R.string.media_refresh), onClick = onRefresh)
            onMore?.let { UiToolbarButton(UiIconKind.MORE, stringResource(R.string.media_more), onClick = it) }
        },
        transparent = true,
    )
}

internal const val MEDIA_TOOLBAR_ALPHA = 0xAA / 255f
