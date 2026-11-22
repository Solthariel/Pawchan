package com.solthariel.pawchan.composer

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.solthariel.pawchan.core.ui.AppDialogStyle
import com.solthariel.pawchan.core.ui.KeepWindowBounds
import com.solthariel.pawchan.core.ui.ModalWindow
import com.solthariel.pawchan.core.ui.focusedWindowBottomPadding
import com.solthariel.pawchan.core.ui.modalLayerSurface

@Composable
internal fun ComposerModal(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    ModalWindow(
        onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        KeepWindowBounds()
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.matchParentSize().pointerInput(onDismiss) { detectTapGestures { onDismiss() } })
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .focusedWindowBottomPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
                        .testTag("composer-modal-surface")
                        .modalLayerSurface(AppDialogStyle.shape),
                    shape = AppDialogStyle.shape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 1f),
                    shadowElevation = 6.dp,
                ) {
                    Column(
                        Modifier
                            .verticalScroll(rememberScrollState(), overscrollEffect = null)
                            .pointerInput(Unit) { detectTapGestures { } },
                    ) { content() }
                }
            }
        }
    }
}
