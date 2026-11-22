package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun AttachmentViewerHeader(
    enabled: Boolean,
    onClose: () -> Unit,
    onRemove: () -> Unit,
    draft: AttachmentViewerDraft,
) {
    Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
        BoxWithConstraints(Modifier.fillMaxWidth().testTag("attachment-header")) {
            val layout = attachmentHeaderLayout(maxWidth.value, LocalDensity.current.fontScale)
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = APP_TOP_BAR_HEIGHT)
                    .padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UiToolbarButton(UiIconKind.BACK, "Назад", enabled = enabled, onClick = onClose)
                if (layout != HeaderLayout.COMPACT) AttachmentHistoryButtons(draft, enabled)
                androidx.compose.foundation.layout
                    .Spacer(Modifier.weight(1f))
                if (layout == HeaderLayout.DIRECT) {
                    UiToolbarButton(
                        UiIconKind.REFRESH,
                        "Сбросить правки",
                        enabled = canResetImage(draft, enabled),
                        onClick = draft::reset,
                    )
                    UiToolbarButton(UiIconKind.DELETE, "Удалить вложение", enabled = enabled, onClick = onRemove)
                } else {
                    AttachmentHeaderMenu(draft, enabled, layout == HeaderLayout.COMPACT, onRemove)
                }
            }
        }
    }
}

@Composable
private fun AttachmentHeaderMenu(
    draft: AttachmentViewerDraft,
    enabled: Boolean,
    showHistory: Boolean,
    onRemove: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        UiToolbarButton(UiIconKind.MORE, "Ещё", enabled = enabled) {
            draft.closeNamePanel()
            open = true
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (showHistory) {
                DropdownMenuItem(text = { Text("Отменить") }, enabled = enabled && draft.editor.canUndo, onClick = {
                    open = false
                    draft.undo()
                })
                DropdownMenuItem(text = { Text("Повторить") }, enabled = enabled && draft.editor.canRedo, onClick = {
                    open = false
                    draft.redo()
                })
            }
            DropdownMenuItem(text = { Text("Сбросить правки") }, enabled = canResetImage(draft, enabled), onClick = {
                open = false
                draft.reset()
            })
            DropdownMenuItem(text = { Text("Удалить вложение") }, enabled = enabled, onClick = {
                open = false
                onRemove()
            })
        }
    }
}

private fun canResetImage(
    draft: AttachmentViewerDraft,
    enabled: Boolean,
): Boolean = enabled && !draft.editor.inGesture && draft.editor.edit != DraftImageEdit()

@Composable
private fun AttachmentHistoryButtons(
    draft: AttachmentViewerDraft,
    enabled: Boolean,
) {
    val colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
    IconButton(
        onClick = draft::undo,
        enabled = enabled && draft.editor.canUndo,
        colors = colors,
        modifier = Modifier.size(48.dp),
    ) {
        Icon(painterResource(R.drawable.ic_editor_undo), "Отменить", modifier = Modifier.size(24.dp))
    }
    IconButton(
        onClick = draft::redo,
        enabled = enabled && draft.editor.canRedo,
        colors = colors,
        modifier = Modifier.size(48.dp),
    ) {
        Icon(painterResource(R.drawable.ic_editor_redo), "Повторить", modifier = Modifier.size(24.dp))
    }
}

internal enum class HeaderLayout { DIRECT, WITH_MENU, COMPACT }

internal fun attachmentHeaderLayout(
    width: Float,
    fontScale: Float,
): HeaderLayout {
    require(fontScale > 0f)
    return when {
        width >= HEADER_ICON_WIDTH * DIRECT_ICON_COUNT + HEADER_INSET -> HeaderLayout.DIRECT
        width >= HEADER_ICON_WIDTH * MENU_ICON_COUNT + HEADER_INSET -> HeaderLayout.WITH_MENU
        else -> HeaderLayout.COMPACT
    }
}

private const val HEADER_ICON_WIDTH = 48
private const val HEADER_INSET = 8
private const val DIRECT_ICON_COUNT = 5
private const val MENU_ICON_COUNT = 4
