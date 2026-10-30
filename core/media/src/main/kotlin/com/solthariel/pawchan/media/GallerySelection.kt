package com.solthariel.pawchan.media

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.solthariel.pawchan.core.media.R
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiTopAppBar

/** Кружок выбора, как в галерее Samsung: пустой белый контур или серая заливка с галочкой. */
@Composable
internal fun GallerySelectionCircle(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(22.dp)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(if (selected) SELECTED_FILL else UNSELECTED_FILL)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) UiIcon(UiIconKind.CHECK, Color.White, Modifier.size(14.dp))
    }
}

/** Кнопка ↗ в углу плитки: открыть файл, не выходя из режима выделения. */
@Composable
internal fun GalleryExpandButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.gallery_open_file)
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp).semantics { contentDescription = description },
    ) {
        Box(
            Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(SELECTED_FILL),
            contentAlignment = Alignment.Center,
        ) {
            UiIcon(UiIconKind.EXPAND, Color.White, Modifier.size(14.dp))
        }
    }
}

/** Верхняя панель режима выделения: [◯ Все] Выбрано: N …… Отмена. */
@Composable
internal fun GallerySelectionToolbar(
    state: GalleryToolbarState,
    actions: GalleryToolbarActions,
) {
    UiTopAppBar(
        title = {
            Text(
                stringResource(R.string.gallery_selected_count, state.selectedCount),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconToggleButton(
                checked = state.allSelected,
                onCheckedChange = { actions.selectAll() },
                modifier = Modifier.semantics { contentDescription = "Выделить все" },
            ) {
                UiIcon(
                    if (state.allSelected) UiIconKind.CHECK else UiIconKind.MEDIA_MULTIPLE,
                    LocalContentColor.current,
                    Modifier.size(24.dp),
                )
            }
        },
        actions = {
            TextButton(onClick = actions.toggleSelection) { Text(stringResource(R.string.gallery_cancel)) }
        },
    )
}

/** Нижняя панель действий над выбранным: подписанные значки. */
@Composable
internal fun GallerySelectionBar(
    enabled: Boolean,
    actions: GallerySelectionBarActions,
) {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            actions.download?.let {
                GalleryBarAction(UiIconKind.SAVE, stringResource(R.string.gallery_download), enabled, it)
            }
            GalleryBarAction(UiIconKind.LINK, stringResource(R.string.gallery_copy_links), enabled, actions.copyLinks)
            actions.favorite?.let {
                GalleryBarAction(UiIconKind.STAR, stringResource(R.string.gallery_favorite), enabled, it)
            }
            actions.remove?.let {
                GalleryBarAction(UiIconKind.CLOSE, stringResource(R.string.gallery_unfavorite), enabled, it)
            }
        }
    }
}

@Composable
private fun RowScope.GalleryBarAction(
    icon: UiIconKind,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.weight(1f)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            UiIcon(icon, LocalContentColor.current, Modifier.size(24.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

internal data class GallerySelectionBarActions(
    val download: (() -> Unit)?,
    val copyLinks: () -> Unit,
    val favorite: (() -> Unit)?,
    /** Убрать выбранное из избранного; null — кнопки нет. */
    val remove: (() -> Unit)? = null,
)

/** Ссылки на все выбранные файлы — по одной в строке. */
internal fun copyGalleryLinks(
    context: Context,
    attachments: List<MediaAttachmentUi>,
) {
    val text = attachments.joinToString("\n") { it.originalUrl }
    context.getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText("links", text))
    Toast.makeText(context, context.getString(R.string.gallery_links_copied), Toast.LENGTH_SHORT).show()
}

private const val SELECTED_FILL_ARGB = 0x99505050
private const val UNSELECTED_FILL_ARGB = 0x1A000000
private val SELECTED_FILL = Color(SELECTED_FILL_ARGB)
private val UNSELECTED_FILL = Color(UNSELECTED_FILL_ARGB)
