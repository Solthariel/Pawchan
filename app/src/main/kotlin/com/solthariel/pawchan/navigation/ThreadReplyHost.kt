package com.solthariel.pawchan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.solthariel.pawchan.composer.ReplyActions
import com.solthariel.pawchan.composer.ReplyComposer
import com.solthariel.pawchan.composer.ReplyComposerState
import com.solthariel.pawchan.composer.StatefulComposerFullScreenHost
import com.solthariel.pawchan.infrastructure.MediaLoaders
import com.solthariel.pawchan.thread.LocalThreadReadingEnabled

@Composable
internal fun ThreadReplyHost(
    state: ReplyComposerState,
    mediaLoaders: MediaLoaders,
    actions: ReplyActions,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalThreadReadingEnabled provides !state.visible) {
        StatefulComposerFullScreenHost(
            visible = state.visible,
            state = state,
            onClose = actions.onClose,
            content = content,
            form = { displayedState -> ReplyComposer(displayedState, mediaLoaders.imageLoader, actions) },
        )
    }
}
