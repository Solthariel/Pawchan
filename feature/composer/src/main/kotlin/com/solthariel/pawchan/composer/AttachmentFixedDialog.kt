package com.solthariel.pawchan.composer

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.solthariel.pawchan.core.ui.KeepWindowBounds

/** Keep the same window bounds while the keyboard opens or closes. */
@Composable
internal fun AttachmentFixedDialog(
    onDismiss: () -> Unit,
    alignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val dismiss by rememberUpdatedState(onDismiss)
        KeepWindowBounds()
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.matchParentSize().pointerInput(Unit) {
                    detectTapGestures { dismiss() }
                },
            )
            Box(
                Modifier.fillMaxSize().systemBarsPadding().padding(16.dp),
                contentAlignment = alignment,
            ) {
                content()
            }
        }
    }
}
