package com.solthariel.pawchan.navigation

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.composer.ComposerFullScreenHost
import com.solthariel.pawchan.composer.ReplyActions
import com.solthariel.pawchan.composer.ReplyComposer
import com.solthariel.pawchan.composer.ReplyComposerState
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.infrastructure.MediaLoaders

@Composable
internal fun ThreadReplyHost(
    board: BoardId,
    state: ReplyComposerState,
    mediaLoaders: MediaLoaders,
    actions: ReplyActions,
    content: @Composable () -> Unit,
) {
    ComposerFullScreenHost(
        visible = state.visible,
        onClose = actions.onClose,
        content = content,
        form = { ReplyComposer(board, state, mediaLoaders.imageLoader, actions) },
    )
}
