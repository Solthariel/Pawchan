package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Both initial placement and the parent viewport are independent of keyboard height. */
@Composable
internal fun StableKeyboardDialog(
    onDismiss: () -> Unit,
    modifier: Modifier,
    properties: DialogProperties,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = properties.dismissOnBackPress,
                dismissOnClickOutside = properties.dismissOnClickOutside,
                securePolicy = properties.securePolicy,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        KeepWindowBounds()
        BoxWithConstraints(
            Modifier.fillMaxSize().systemBarsPadding().padding(AppDialogStyle.screenPadding),
            contentAlignment = Alignment.Center,
        ) {
            DialogBackdrop(Modifier.matchParentSize(), properties.dismissOnClickOutside, onDismiss)
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
