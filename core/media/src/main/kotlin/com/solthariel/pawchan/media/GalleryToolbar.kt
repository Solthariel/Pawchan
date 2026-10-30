package com.solthariel.pawchan.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiTopAppBar

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
    Column {
        UiTopAppBar(
            title = { Text("Галерея", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                GalleryIconButton("Назад", actions.back) {
                    UiIcon(UiIconKind.BACK, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(24.dp))
                }
            },
            actions = {
                GalleryIconButton("Выделить медиа", actions.toggleSelection) {
                    GallerySelectIcon(MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            GalleryFilter.entries.forEach { filter ->
                GalleryFilterButton(filter, filter.group(gallery).count, filter == state.filter) {
                    actions.chooseFilter(filter)
                }
            }
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
    FilterChip(
        selected = active,
        onClick = onClick,
        label = { Text(count.toString()) },
        leadingIcon = { GalleryFilterIcon(filter, MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.semantics { contentDescription = "${filter.label}: $count" },
    )
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
