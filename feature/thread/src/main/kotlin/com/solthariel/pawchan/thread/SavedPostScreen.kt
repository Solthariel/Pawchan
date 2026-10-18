package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.rememberMediaSaveActions
import com.solthariel.pawchan.media.thumbnail.rememberThumbnailLoading

/** No mutable thread session is passed back to the app. Slots get only their close callback. */
data class SavedPostActions(
    val onReadReplies: () -> Unit,
)

data class SavedPostResources(
    val media: ThreadMediaResources,
    val preview: PostPreviewResources,
)

@Composable
fun SavedPostScreen(
    item: SavedPostPresentation,
    resources: SavedPostResources,
    actions: SavedPostActions,
) {
    val media = resources.media
    val loading =
        rememberThumbnailLoading(
            media.thumbnails,
            androidx.compose.foundation.lazy
                .rememberLazyListState(),
        )
    val save = rememberMediaSaveActions(media.saves)
    var menu by remember(item.address) { mutableStateOf(false) }
    var stack by remember(item.address) { mutableStateOf<List<PostPreviewTarget>>(emptyList()) }
    var opened by remember(item.address) { mutableStateOf<MediaAttachmentUi?>(null) }
    val preview: (PostPreviewTarget) -> Unit = { stack = stack + it }
    item.Card(
        loading,
        ThreadAttachmentActions({ opened = it }, save.request, {}),
        SavedPostInteractions({ menu = true }, {
            actions.onReadReplies()
            preview(PostPreviewTarget(item.address.thread, item.address.post.value, true))
        }, { preview(PostPreviewTarget(it.thread ?: item.address.thread, it.post.value)) }),
    )
    stack.lastOrNull()?.let { target ->
        item.Preview(
            target,
            PostPreviewBindings(
                { stack = stack.dropLast(1) },
                { preview(PostPreviewTarget(it.thread ?: item.address.thread, it.post.value)) },
                { preview(PostPreviewTarget(target.address, it, true)) },
                { opened = it },
                save.request,
            ),
            resources.preview,
        )
    }
    if (menu) {
        val close = { menu = false }
        item.Menu(resources.preview.actions, { stack = emptyList() }, close)
    }
    SavedPostMediaViewer(opened, item.attachments, media.viewer, save) { opened = null }
}
