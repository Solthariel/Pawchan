package com.solthariel.pawchan.composer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Only actions are pinned; attachments belong to the editor's single scrollable body. */
@Composable
internal fun ComposerEditorDock(
    state: ComposerScreenState,
    newThread: Boolean,
    onAttach: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier,
) {
    ComposerActionPanel(state, newThread, onAttach, onSubmit, modifier)
}
