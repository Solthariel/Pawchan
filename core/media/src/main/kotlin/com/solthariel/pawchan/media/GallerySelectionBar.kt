package com.solthariel.pawchan.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.media.R
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind

/** Compact action surface, not a destination navigation bar. Insets belong to the gallery host. */
@Composable
internal fun GallerySelectionBar(
    enabled: Boolean,
    actions: GallerySelectionBarActions,
) {
    Surface(modifier = Modifier.testTag("gallery-selection-bar"), color = MaterialTheme.colorScheme.surfaceContainer) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val count = 1 + listOfNotNull(actions.download, actions.favorite, actions.remove).size
            val labels = count <= 2 && maxWidth / LocalDensity.current.fontScale >= 320.dp
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions.download?.let {
                    GalleryBarAction(UiIconKind.SAVE, stringResource(R.string.gallery_download), enabled, labels, it)
                }
                GalleryBarAction(
                    UiIconKind.LINK,
                    stringResource(R.string.gallery_copy_links),
                    enabled,
                    labels,
                    actions.copyLinks,
                )
                actions.favorite?.let {
                    GalleryBarAction(UiIconKind.STAR, stringResource(R.string.gallery_favorite), enabled, labels, it)
                }
                actions.remove?.let {
                    GalleryBarAction(UiIconKind.CLOSE, stringResource(R.string.gallery_unfavorite), enabled, labels, it)
                }
            }
        }
    }
}

@Composable
private fun RowScope.GalleryBarAction(
    icon: UiIconKind,
    label: String,
    enabled: Boolean,
    showLabel: Boolean,
    onClick: () -> Unit,
) {
    val modifier =
        (if (showLabel) Modifier.weight(1f) else Modifier)
            .heightIn(min = 48.dp)
            .semantics { contentDescription = label }
    if (showLabel) {
        TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
            UiIcon(icon, androidx.compose.material3.LocalContentColor.current, Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    } else {
        IconButton(onClick = onClick, modifier = modifier.size(48.dp), enabled = enabled) {
            UiIcon(icon, androidx.compose.material3.LocalContentColor.current, Modifier.size(24.dp))
        }
    }
}

internal data class GallerySelectionBarActions(
    val download: (() -> Unit)?,
    val copyLinks: () -> Unit,
    val favorite: (() -> Unit)?,
    /** Remove selected favourites; null means the action is unavailable. */
    val remove: (() -> Unit)? = null,
)
