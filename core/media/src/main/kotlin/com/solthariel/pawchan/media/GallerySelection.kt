package com.solthariel.pawchan.media

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import com.solthariel.pawchan.core.media.R
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind

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
    Box(
        modifier
            .size(36.dp)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
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
    Row(
        Modifier
            .testTag(
                "gallery-toolbar",
            ).fillMaxWidth()
            .heightIn(min = APP_TOP_BAR_HEIGHT)
            .padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GalleryIconButton(stringResource(R.string.gallery_cancel), actions.toggleSelection) {
            UiIcon(UiIconKind.BACK, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(24.dp))
        }
        Text(
            stringResource(R.string.gallery_selected_count, state.selectedCount),
            Modifier.weight(1f).padding(start = 12.dp),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        GalleryIconButton(stringResource(R.string.gallery_all), actions.selectAll) {
            GallerySelectionCircle(state.allSelected, Modifier.border(1.dp, OUTLINE, CircleShape))
        }
    }
}

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
private const val OUTLINE_ARGB = 0x33000000
private val SELECTED_FILL = Color(SELECTED_FILL_ARGB)
private val UNSELECTED_FILL = Color(UNSELECTED_FILL_ARGB)
private val OUTLINE = Color(OUTLINE_ARGB)
