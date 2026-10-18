package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ComposerFullScreenHost(
    visible: Boolean,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
    form: @Composable () -> Unit,
) {
    content()
    if (visible) {
        Dialog(
            onDismissRequest = onClose,
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { form() }
        }
    }
}
