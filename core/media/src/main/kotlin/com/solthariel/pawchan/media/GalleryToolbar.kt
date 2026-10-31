package com.solthariel.pawchan.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        Modifier.fillMaxWidth().height(APP_TOP_BAR_HEIGHT).padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GalleryIconButton("Назад", actions.back) {
            UiIcon(UiIconKind.BACK, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(24.dp))
        }
        Text(
            "Галерея",
            Modifier.weight(1f).padding(start = 4.dp),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        GalleryFilter.entries.forEach { filter ->
            GalleryFilterButton(filter, filter.group(gallery).count, filter == state.filter) {
                actions.chooseFilter(filter)
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
            .size(44.dp)
            .clip(CircleShape)
            .semantics {
                contentDescription = "${filter.label}: $count"
                selected = active
            }.clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GalleryFilterIcon(filter, color)
        Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1)
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
