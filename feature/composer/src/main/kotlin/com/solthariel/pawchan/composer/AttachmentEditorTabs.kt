package com.solthariel.pawchan.composer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.solthariel.pawchan.feature.composer.R

/** File options are replaced in place by equally sized tool tiles. */
@Composable
internal fun AttachmentEditorTabs(
    draft: AttachmentViewerDraft,
    enabled: Boolean,
) {
    AttachmentControlRow(EDITOR_ACTIONS.size + 1, Modifier.testTag("editor-tool-tabs")) {
        EDITOR_ACTIONS.forEach { (label, tab) ->
            AttachmentOptionTile(
                AttachmentTileState(
                    label,
                    draft.tab == tab && tab != AttachmentEditorTab.FILE,
                    enabled && !draft.editor.inGesture,
                    action = true,
                ),
                Modifier.weight(1f),
            ) { draft.selectTab(tab) }
        }
        AttachmentOptionTile(
            AttachmentTileState(
                R.string.editor_compare,
                draft.comparing,
                enabled && !draft.editor.inGesture,
                action = true,
            ),
            Modifier.weight(1f),
        ) { draft.comparing = !draft.comparing }
    }
}

private val EDITOR_ACTIONS =
    listOf(
        R.string.editor_back to AttachmentEditorTab.FILE,
        R.string.editor_crop to AttachmentEditorTab.CROP,
        R.string.editor_rotate to AttachmentEditorTab.ROTATE,
        R.string.editor_draw to AttachmentEditorTab.DRAW,
        R.string.editor_redact to AttachmentEditorTab.REDACT,
    )
