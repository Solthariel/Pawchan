package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** A window-scoped binding; the local and its consumers remain private to the feature. */
@Composable
fun ThreadPostLinkHost(
    bindings: PostLinkBindings,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalPostLinks provides bindings, content = content)
}
