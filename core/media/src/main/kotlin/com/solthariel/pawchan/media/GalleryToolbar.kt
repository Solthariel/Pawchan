package com.solthariel.pawchan.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind

@Composable
internal fun GalleryToolbar(
    state: GalleryToolbarState,
    actions: GalleryToolbarActions,
    gallery: MediaGalleryContent,
) {
    if (state.selecting) {
        GallerySelectionToolbar(state, actions)
        return
    }
    Row(
        Modifier
            .testTag("gallery-toolbar")
            .fillMaxWidth()
            .heightIn(min = APP_TOP_BAR_HEIGHT)
            .padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GalleryIconButton("Назад", actions.back) {
            UiIcon(UiIconKind.BACK, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(24.dp))
        }
        Text(
            "Галерея",
            Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                GalleryFilter.entries.forEach { filter ->
                    GalleryFilterButton(filter, filter.group(gallery).count, filter == state.filter) {
                        actions.chooseFilter(filter)
                    }
                }
            }
        }
        GalleryIconButton("Выделить медиа", actions.toggleSelection) {
            GallerySelectIcon(MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun GalleryFilterButton(
    filter: GalleryFilter,
    count: Int,
    active: Boolean,
    onClick: () -> Unit,
) {
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .clip(CircleShape)
            .semantics {
                contentDescription = "${filter.label}: $count"
                selected = active
            }.clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GalleryFilterIcon(filter, color)
        Text(
            count.toString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

internal fun GalleryFilter.group(gallery: MediaGalleryContent): MediaGalleryGroup =
    when (this) {
        GalleryFilter.ALL -> gallery.all
        GalleryFilter.IMAGES -> gallery.images
        GalleryFilter.GIF -> gallery.animations
        GalleryFilter.VIDEO -> gallery.videos
    }

internal enum class GalleryFilter(
    val label: String,
) {
    ALL("Все"),
    IMAGES("Изображения"),
    GIF("GIF"),
    VIDEO("Видео"),
}

internal data class GalleryToolbarState(
    val filter: GalleryFilter,
    val selecting: Boolean,
    val selectedCount: Int,
    val allSelected: Boolean = false,
)

internal data class GalleryToolbarActions(
    val back: () -> Unit,
    val chooseFilter: (GalleryFilter) -> Unit,
    val toggleSelection: () -> Unit,
    val selectAll: () -> Unit = {},
)
