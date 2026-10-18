package com.solthariel.pawchan.thread

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal class PostTextSelection(
    private val owner: PostSelectionOwner?,
    private val toolbar: TextToolbar,
) {
    var range by mutableStateOf(TextRange.Zero)
    var bounds = Rect.Zero
    var focused = false
    val focus = FocusRequester()
    private var menuShown = false

    fun select(value: TextRange) {
        if (value.collapsed) {
            clear()
        } else {
            owner?.select(this, { bounds }, ::clear)
            range = value
        }
    }

    fun clear() {
        if (menuShown) toolbar.hide()
        menuShown = false
        range = TextRange.Zero
        owner?.release(this)
    }

    fun showingMenu() {
        menuShown = true
    }
}

@Composable
internal fun rememberPostTextSelection(
    key: Any,
    text: String,
): PostTextSelection {
    val owner = LocalPostSelectionOwner.current
    val toolbar = LocalTextToolbar.current
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val selection = remember(key, owner, toolbar) { PostTextSelection(owner, toolbar) }
    LaunchedEffect(selection.range) {
        if (!selection.range.collapsed) {
            selection.focus.requestFocus()
            selection.showingMenu()
            showParagraphToolbar(
                context,
                toolbar,
                selection.bounds,
                text.substring(selection.range.min, selection.range.max),
                selection::clear,
            )
        }
    }
    DisposableEffect(selection, lifecycle) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) selection.clear()
            }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            selection.clear()
        }
    }
    BackHandler(enabled = !selection.range.collapsed, onBack = selection::clear)
    return selection
}
