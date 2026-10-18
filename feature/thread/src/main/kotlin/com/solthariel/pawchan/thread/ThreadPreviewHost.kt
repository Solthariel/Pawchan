package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.solthariel.pawchan.media.MediaSaveActions

@Composable
internal fun ThreadPreviewHost(
    thread: ThreadUiState.Content,
    memory: ThreadScreenMemory,
    save: MediaSaveActions,
    presentation: ThreadPresentation,
    resources: PostPreviewResources?,
) {
    val tools = memory.tools
    LaunchedEffect(tools.replyPreviewPostId) {
        tools.replyPreviewPostId?.let {
            tools.previews = tools.previews + PostPreviewTarget(presentation.postContext?.address, it, replies = true)
            tools.replyPreviewPostId = null
        }
    }
    if (tools.previews.isEmpty() || resources == null) return
    PostPreviewStack(
        tools.previews,
        thread,
        memory.appearance,
        resources,
        bindings = { target -> previewBindings(target, thread, memory, save, presentation) },
        onClear = { tools.previews = emptyList() },
    )
}

@Composable
private fun previewBindings(
    target: PostPreviewTarget,
    thread: ThreadUiState.Content,
    memory: ThreadScreenMemory,
    save: MediaSaveActions,
    presentation: ThreadPresentation,
): PostPreviewBindings {
    val tools = memory.tools
    val openThread = LocalPostLinks.current?.onOpenThread
    val screenThread = presentation.postContext?.address ?: thread.address
    val foreign = target.address?.takeIf { !target.replies && it != screenThread }
    return PostPreviewBindings(
        onClose = { tools.previews = tools.previews.dropLast(1) },
        onReference = { ref ->
            tools.previews =
                tools.previews +
                PostPreviewTarget(
                    ref.thread ?: target.address ?: presentation.postContext?.address,
                    ref.post.value,
                )
        },
        onReplies = { id ->
            tools.previews = tools.previews + PostPreviewTarget(target.address, id, replies = true)
        },
        // Просмотрщик открывается окном поверх превью: цепочка превью остаётся на месте.
        onOpen = { memory.opened = it },
        onDownload = save.request,
        onReply = { tools.previews = emptyList() },
        onOpenThread =
            if (foreign != null && openThread != null) {
                { address, post ->
                    tools.previews = emptyList()
                    openThread(address, post.takeIf { it != address.thread.value })
                }
            } else {
                null
            },
    )
}
