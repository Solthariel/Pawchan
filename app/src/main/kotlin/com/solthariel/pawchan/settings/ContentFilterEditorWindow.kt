package com.solthariel.pawchan.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import com.solthariel.pawchan.core.ui.FullScreenModalWindow as Dialog

@Composable
internal fun FilterEditorWindow(
    onClose: () -> Unit,
    onDelete: (() -> Unit)?,
    confirm: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        FilterEditorScreen(onClose, onDelete, confirm, content)
    }
}
