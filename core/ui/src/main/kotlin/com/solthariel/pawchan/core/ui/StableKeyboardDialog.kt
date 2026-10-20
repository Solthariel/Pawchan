package com.solthariel.pawchan.core.ui

import android.view.WindowManager
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/** Upper input panel keeps its origin when the IME opens; only its scrollable height may shrink. */
@Composable
internal fun StableKeyboardDialog(
    onDismiss: () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Dialog(onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            val previous = window?.attributes?.softInputMode
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
            onDispose { previous?.let { window.setSoftInputMode(it) } }
        }
        val density = LocalDensity.current
        val keyboard = with(density) { WindowInsets.ime.getBottom(this).toDp() }
        BoxWithConstraints(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = AppDialogStyle.screenPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val top = minOf(INPUT_TOP_INSET, maxHeight / INPUT_TOP_DIVISOR)
            Surface(
                modifier
                    .padding(top = top)
                    .widthIn(max = AppDialogStyle.maxWidth)
                    .fillMaxWidth()
                    .heightIn(max = (maxHeight - top - keyboard - 8.dp).coerceAtLeast(0.dp))
                    .testTag("app-dialog"),
                shape = AppDialogStyle.shape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                content = content,
            )
        }
    }
}

private const val INPUT_TOP_DIVISOR = 8
private val INPUT_TOP_INSET = 64.dp
