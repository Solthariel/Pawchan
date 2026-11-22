package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable

@Composable
internal fun PreviewPostMenu(
    post: ThreadPostUi,
    content: ThreadUiState.Content,
    actions: PostPreviewActions,
    onReply: () -> Unit,
    onClose: () -> Unit,
) {
    val address = content.address ?: return
    val bindings = LocalPostLinks.current ?: return
    val links = bindings.resolver
    PreviewMenuDialog(!content.closed && actions.canReply(address), { command ->
        executePreviewPostMenu(command, post, content, actions, bindings.transfer, onReply)
        onClose()
    }, onClose, links.postUrl(address, post.id) != null)
}
