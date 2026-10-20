package com.solthariel.pawchan.settings

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.AppDialogStyle

/** A full-size transparent host keeps the compact surface anchored above the IME, never centered. */
@Composable
internal fun FilterEditorViewport(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInsets: WindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime),
    content: @Composable () -> Unit,
) {
    val topInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    Box(modifier.fillMaxSize()) {
        Box(Modifier.matchParentSize().pointerInput(onClose) { detectTapGestures { onClose() } })
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(topInsets)
                .windowInsetsPadding(bottomInsets.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Surface(
                Modifier
                    .widthIn(max = AppDialogStyle.maxWidth)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .testTag("filter-editor-window"),
                shape = AppDialogStyle.shape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                content = content,
            )
        }
    }
}
