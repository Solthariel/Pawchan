package com.solthariel.pawchan.composer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/** This dedicated window is discarded on close; keep both enter and exit free of platform fades. */
@Composable
internal fun AttachmentViewerWindow() {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window
    SideEffect { window?.setWindowAnimations(0) }
}
