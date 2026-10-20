package com.solthariel.pawchan.core.ui

import android.view.WindowManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.window.DialogWindowProvider

/** Each dialog owns its mode; nested dialogs never change their parent's window attributes. */
@Composable
fun KeepWindowBounds() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.parent as? DialogWindowProvider)?.window
        val previous = window?.attributes?.softInputMode
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        onDispose { previous?.let { window.setSoftInputMode(it) } }
    }
}

/** The active editor may avoid IME; an unfocused background editor keeps its last inset. */
@Composable
fun Modifier.focusedWindowImePadding(): Modifier {
    val density = LocalDensity.current
    val focused = LocalWindowInfo.current.isWindowFocused
    val ime = WindowInsets.ime.getBottom(density)
    var lastFocusedInset by remember { mutableIntStateOf(ime) }
    SideEffect { if (focused) lastFocusedInset = ime }
    return padding(bottom = with(density) { (if (focused) ime else lastFocusedInset).toDp() })
}
