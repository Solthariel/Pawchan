package com.solthariel.pawchan.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties

/** Shared fixed viewport. IME overlays windows instead of recentering the dialog stack. */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    StableKeyboardDialog(onDismissRequest, modifier, properties, content)
}
