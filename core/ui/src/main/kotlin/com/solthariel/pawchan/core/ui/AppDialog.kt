package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Shared centered window. Full-screen viewers and anchored overflow menus have separate contracts. */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    keyboardStable: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (keyboardStable) {
        StableKeyboardDialog(onDismissRequest, modifier, content)
        return
    }
    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val focused = LocalWindowInfo.current.isWindowFocused
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .then(if (focused) Modifier.imePadding() else Modifier)
                .padding(AppDialogStyle.screenPadding),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier
                    .widthIn(max = AppDialogStyle.maxWidth)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .testTag("app-dialog"),
                shape = AppDialogStyle.shape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                content = content,
            )
        }
    }
}
